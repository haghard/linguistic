package linguistic.ps.pruningRadixTrie

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import linguistic.protocol.SearchQuery.WordsQuery

import java.io.File
import linguistic.protocol.{AddOneWord, OneWordAdded, SearchQuery, SearchResults, UniqueTermsByShard2}
import linguistic.pruningRadixTrie.JPruningRadixTrie
import linguistic.ps.Indexing
import linguistic.ps.pruningRadixTrie.PruningRadixTrieEntity.mbDivider
import org.openjdk.jol.info.GraphLayout

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import scala.util.control.NonFatal

/**
  * Each PruningRadixTrieEntity uses [[JPruningRadixTrie]] to store his state.
  * Filtering by prefix is extremely fast with a radix tree (worst case O(log(N)),
  * whereas it is worse than O(N) with SortedMap and HashMap.
  * Filtering by prefix will also benefit a lot from structural sharing.
  */
object PruningRadixTrieEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(index: JPruningRadixTrie)

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

  def props(): Props =
    Props(new PruningRadixTrieEntity()).withDispatcher("shard-dispatcher")
}

class PruningRadixTrieEntity extends PersistentActor with ActorLogging with Indexing[Unit] with Stash {

  //val path             = "./words.txt"
  val path = "./terms.txt"
  //val path           = "./list_of_english_words.txt"
  //val snapshotFilePath = s"./PruningRadixTrie/$key.txt"

  override def key           = self.path.name
  override def persistenceId = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("PruningRadixTrieEntity({}) has been stopped", key)

  override def receiveCommand: Receive = {
    case m: PruningRadixTrieEntity.RestoredIndex =>
      val size = m.index.termCount
      if (size == 0) {
        buildIndex(self.toTyped[Indexing.IndexingProtocol], key, path)
        context.become(indexing(scala.collection.mutable.HashSet.empty[String]))
      } else {
        unstashAll()
        log.info(
          s"Index has been recovered from snapshot for key=$key. Size:${m.index.termCount}"
        )
        context.become(active(m.index))
      }
    case c =>
      log.info(s"Stash: $c")
      stash()
  }

  def indexing(unqWdsSet: scala.collection.mutable.HashSet[String]): Receive = {
    case cmd: Indexing.IndexingProtocol =>
      cmd match {
        case Indexing.IndexingProtocol.Init(replyTo) =>
          replyTo.tell(Indexing.Confirm)

        case Indexing.IndexingProtocol.Next(replyTo, words) =>
          words.foreach(unqWdsSet.add(_))
          replyTo.tell(Indexing.Confirm)
          context.become(indexing(unqWdsSet))

        case Indexing.IndexingProtocol.OnCompleted =>
          val index = new JPruningRadixTrie()
          unqWdsSet.foreach { term =>
            try index.addTerm(term, 1)
            catch {
              case NonFatal(ex) =>
                log.warning(s"Failed to add $term. " + ex.getMessage)
            }
          }

          if (unqWdsSet.nonEmpty) {
            log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, unqWdsSet.size)
            saveSnapshot(UniqueTermsByShard2(unqWdsSet))
            //index.writeTermsToFile(snapshotFilePath)
          }

          unstashAll()
          log.info("Becomes active for search {}: {}/{}", key, unqWdsSet.size, index.termCount)
          context.become(active(index))

        case Indexing.IndexingProtocol.Failure(ex) =>
          throw ex
      }

    case c =>
      log.info("{} is indexing right now. stashing {} ", key, c)
      stash()
  }

  def active(index: JPruningRadixTrie): Receive = {
    case AddOneWord(word) =>
      persist(OneWordAdded(word)) { ev =>
        index.addTerm(word, 1)
        context.become(active(index))
      }

    case WordsQuery(prefix, maxResults, replyTo) =>
      val decodedPrefix = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)

      val startTs = System.currentTimeMillis()
      val iter    = index.getTopkTermsForPrefix(decodedPrefix, maxResults).iterator()
      //Thread.sleep(java.util.concurrent.ThreadLocalRandom.current().nextInt(200, 600))
      val latency = System.currentTimeMillis() - startTs
      val buf     = Vector.newBuilder[String]
      while (iter.hasNext) {
        val elem = iter.next()
        buf += elem.getTerm()
      }
      val results = buf.result()

      log.info(s"Search($prefix) over ${index.termCount}term resulted in [${results.size}]. $latency millis")
      replyTo.tell(SearchResults(results))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr $key")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    val recoveredIndex = new JPruningRadixTrie()

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard2) =>
        snapshot.terms.foreach(recoveredIndex.addTerm(_, 1))

      case OneWordAdded(w) =>
        recoveredIndex.addTerm(w, 1)

      case RecoveryCompleted =>
        val mb1 = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
        log.info(s"************* Recovered index by key: ${key} count:${recoveredIndex.termCount} - Size:$mb1 mb")
        self ! PruningRadixTrieEntity.RestoredIndex(recoveredIndex)
    }
  }
}

/*
 l.p.p.PruningRadixTrieEntity - ************* Recovered index by key: r count:21285 - Size:3.23246 mb
 */
