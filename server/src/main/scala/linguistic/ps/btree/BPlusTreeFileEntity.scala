package linguistic.ps.btree

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import linguistic.protocol.SearchQuery.WordsQuery

import java.io.File
import linguistic.protocol.{AddOneWord, OneWordAdded, SearchQuery, SearchResults, UniqueTermsByShard2}
import linguistic.ps.Indexing
import linguistic.ps.pruningRadixTrie.PruningRadixTrieEntity.mbDivider
import org.openjdk.jol.info.GraphLayout

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import scala.util.control.NonFatal
import xyz.hyperreal.btree.FileBPlusTree

object BPlusTreeFileEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(index: FileBPlusTree[String, Null])

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
    Props(new BPlusTreeFileEntity()).withDispatcher("shard-dispatcher")
}

class BPlusTreeFileEntity extends PersistentActor with ActorLogging with Indexing[Unit] with Stash {

  //val path             = "./words.txt"
  val path    = "./terms.txt"
  val ramFile = s"./btree/$key"

  //val path           = "./list_of_english_words.txt"

  override def key           = self.path.name
  override def persistenceId = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("BTreeEntity({}) has been stopped", key)

  override def receiveCommand: Receive = {
    case m: BPlusTreeFileEntity.RestoredIndex =>
      if (m.index.isEmpty) {
        buildIndex(self.toTyped[Indexing.IndexingProtocol], key, path)
        context.become(indexing(new scala.collection.mutable.HashSet[String]))
      } else {
        unstashAll()
        log.info(
          s"Index has been recovered from snapshot for key=$key. Size:${m.index.keys.size}"
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
          words.foreach(w => if (w.nonEmpty) unqWdsSet.add(w))
          replyTo.tell(Indexing.Confirm)
          context.become(indexing(unqWdsSet))

        case Indexing.IndexingProtocol.OnCompleted =>
          val index = new FileBPlusTree[String, Null](ramFile, 7)

          try index.insertKeys(unqWdsSet.toVector: _*)
          catch {
            case NonFatal(ex) =>
              log.warning(s"Failed to add " + ex.getMessage)
              Thread.sleep(1000)
          }

          /*try unqWdsSet.foreach(k ⇒ index.insertKeys(k)) catch {
            case NonFatal(ex) ⇒
              log.warning(s"Failed to add " + ex.getMessage)
          }*/

          if (!unqWdsSet.isEmpty) {
            log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, unqWdsSet.size)
            saveSnapshot(UniqueTermsByShard2(unqWdsSet))
          }

          unstashAll()
          log.info("Becomes active for search {}: {}", key, unqWdsSet.size)
          context.become(active(index))

        case Indexing.IndexingProtocol.Failure(ex) =>
          throw ex
      }

    case c =>
      log.info("{} is indexing right now. stashing {} ", key, c)
      stash()
  }

  def active(index: FileBPlusTree[String, Null]): Receive = {
    case AddOneWord(word) =>
      persist(OneWordAdded(word)) { ev =>
        index.insertKeys(word)
        context.become(active(index))
      }

    case WordsQuery(prefix, maxResults, replyTo) =>
      val resBuf = Vector.newBuilder[String]

      val from = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)
      val to   = from + String.valueOf('z')

      val startTs = System.currentTimeMillis()
      val iter    = index.boundedIterator((Symbol(">="), from), (Symbol("<"), to))
      val latency = System.currentTimeMillis() - startTs

      var i = 0
      while (iter.hasNext && i <= maxResults) {
        val elem = iter.next()._1
        resBuf.+=(elem)
        i = i + 1
      }

      log.info(s"Search[>=$prefix...<$to) term resulted in [$i] $latency millis")
      val results = resBuf.result()
      replyTo.tell(SearchResults(results))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr $key")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    val recoveredIndex = new FileBPlusTree[String, Null](ramFile, 7)

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard2) =>
        recoveredIndex.insertKeys(snapshot.terms.toVector: _*)

      case OneWordAdded(w) =>
        recoveredIndex.insertKeys(w)

      case RecoveryCompleted =>
        val mb1 = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
        log.info(s"************* Recovered index by key: ${key} count:${recoveredIndex.keys.size} - Size:$mb1 mb")
        self ! BPlusTreeFileEntity.RestoredIndex(recoveredIndex)
    }
  }
}
