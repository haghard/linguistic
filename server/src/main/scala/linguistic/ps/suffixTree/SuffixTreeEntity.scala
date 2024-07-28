package linguistic.ps

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}

import java.io.File
import com.abahgat.suffixtree.GeneralizedSuffixTree
import linguistic.protocol._

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import scala.util.control.NonFatal
import scala.collection.mutable

object SuffixTreeEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat // 1_048_576

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(
    index: GeneralizedSuffixTree,
    uniqueWords: Vector[String]
  )

  val extractShardId: ExtractShardId = {
    case x: SearchQuery.WordsQuery =>
      x.keyword.toLowerCase(Locale.ROOT).take(1) // shards: [a,...,z]
    case x: AddOneWord =>
      x.w.toLowerCase(Locale.ROOT).take(1)
    case ShardRegion.StartEntity(id) =>
      id
  }

  val extractEntityId: ExtractEntityId = {
    case x: SearchQuery.WordsQuery =>
      (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
    case x: AddOneWord =>
      (x.w.toLowerCase(Locale.ROOT).take(1), x)
  }

  def props(isPrefixBasedSearch: Boolean): Props =
    Props(new SuffixTreeEntity(isPrefixBasedSearch)).withDispatcher("shard-dispatcher")

}

class SuffixTreeEntity(isPrefixBasedSearch: Boolean)
    extends PersistentActor
    with ActorLogging
    with Indexing[Unit]
    with Stash
    with Passivation {

  override val key = self.path.name

  // val path = "./words.txt"
  // val path = "./terms.txt"
  private def path = s"./PruningRadixTrie/$key.txt"

  override def persistenceId: String = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("{} has been stopped", key)

  override def receiveCommand: Receive = {
    case m: SuffixTreeEntity.RestoredIndex =>
      val size = m.uniqueWords.size
      if (size == 0) {
        buildIndex(self.toTyped[Indexing.IndexingProtocol], key, path)
        context.become(indexing(mutable.HashSet.empty[String]))
      } else {
        unstashAll()
        log.info(
          s"Index has been recovered from snapshot with $size terms for key=$key. ${m.index.computeCount()}/${m.uniqueWords.size}"
        )
        context.become(active(m.index, m.uniqueWords))
      }
    case c =>
      log.info(s"Stash: $c")
      stash()
  }

  def indexing(unqWdsSet: mutable.HashSet[String]): Receive = {
    case cmd: Indexing.IndexingProtocol =>
      cmd match {
        case Indexing.IndexingProtocol.Init(replyTo) =>
          replyTo.tell(Indexing.Confirm)

        case Indexing.IndexingProtocol.Next(replyTo, words) =>
          // val u = words.foldLeft(unqWdsSet) { (acc, w) ⇒ acc + w.trim }
          words.foreach(unqWdsSet.add(_))
          replyTo.tell(Indexing.Confirm)
          context.become(indexing(unqWdsSet))

        case Indexing.IndexingProtocol.OnCompleted =>
          if (unqWdsSet.nonEmpty) {
            log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, unqWdsSet.size)
            saveSnapshot(UniqueTermsByShard2(unqWdsSet))
          }
          unstashAll()

          var i     = 0
          val index = new GeneralizedSuffixTree()
          val buf   = Vector.newBuilder[String]
          unqWdsSet.foreach { term =>
            try {
              index.put(term, i)
              buf.+=(term)
              i = i + 1
            } catch {
              case NonFatal(ex) =>
                log.warning(s"Failed to add $term at $i " + ex.getMessage)
            }
          }

          log.info("Becomes active for search {}: {}/{}", key, unqWdsSet.size, index.computeCount())
          context.become(active(index, buf.result()))

        case Indexing.IndexingProtocol.Failure(ex) =>
          throw ex
      }

    // case SearchQuery.WordsQuery(prefix, _) case cmd: AddOneWord ⇒
    case c =>
      log.info("{} is indexing right now. stashing {} ", key, c)
      stash()
  }

  def active(index: GeneralizedSuffixTree, uniqueWords: Vector[String]): Receive = {
    case AddOneWord(w) =>
      persist(OneWordAdded(w)) { ev =>
        val updatedShardUniqueWords = uniqueWords.:+(w)
        index.put(w, updatedShardUniqueWords.size - 1)
        context.become(active(index, updatedShardUniqueWords))
      }

    case SearchQuery.WordsQuery(prefix, maxResultSize, replyTo) =>
      val decodedPrefix = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)

      val buf = Vector.newBuilder[String]

      var i       = 0
      val startTs = System.nanoTime
      val it      = index.search(decodedPrefix).iterator()
      while (it.hasNext() && i < maxResultSize) {
        val ind       = it.next()
        val candidate = uniqueWords(ind)
        if (isPrefixBasedSearch) {
          if (candidate.startsWith(prefix)) {
            buf.+=(candidate)
            i = i + 1
          } else {
            log.info(s"Search by {}: ignore {}", prefix, candidate)
          }
        } else {
          buf.+=(candidate)
          i = i + 1
        }
      }

      val searchRes = buf.result()
      log.info(
        s"Search($prefix) = [${searchRes.size}]. ${TimeUnit.NANOSECONDS
            .toMillis(System.nanoTime - startTs)} millis. ${uniqueWords.size}/${index.computeCount()}"
      )
      replyTo.tell(SearchResults(searchRes))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    val recoveredIndex                 = new GeneralizedSuffixTree()
    var recoveredWords: Vector[String] = Vector.empty

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard2) =>
        val startTs = System.currentTimeMillis()
        var i       = 0
        snapshot.terms.foreach { term =>
          try {
            // if (ThreadLocalRandom.current().nextDouble() > .99995) println(s"$term: $term")
            recoveredIndex.put(term, i)
            i = i + 1
          } catch {
            case NonFatal(ex) =>
              log.warning(s"Failed to recover $term in pos:$i " + ex.getMessage)
          }
        }

        recoveredWords = snapshot.terms.toVector
        val lat = (System.currentTimeMillis() - startTs) / 1000
        log.info(s"SnapshotOffer $meta: ${snapshot.terms.size} terms. Latency:${lat}sec")

      case OneWordAdded(w) =>
        recoveredWords = recoveredWords.:+(w)
        recoveredIndex.put(w, recoveredWords.size - 1)

      case RecoveryCompleted =>
        log.info(s"Recovered index by key: ${key} count:${recoveredIndex.computeCount()}")
        self ! SuffixTreeEntity.RestoredIndex(recoveredIndex, recoveredWords)
    }
  }
}
