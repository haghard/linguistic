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
import xyz.hyperreal.btree.MemoryBPlusTree
import BPlusTreeEntity._

object BPlusTreeEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat

  // (maximum number of branches in an internal node)
  val branchingFactor = 16

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(index: MemoryBPlusTree[String, Null])

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

  case class RowData(url: String, title: String, text: String)

  def props(): Props =
    Props(new BPlusTreeEntity()).withDispatcher("shard-dispatcher")
}

class BPlusTreeEntity extends PersistentActor with ActorLogging with Indexing[Unit] with Stash {

  // val path = "./words.txt"
  val path = "./terms.txt"
  // val path  = "./list_of_english_words.txt"

  override def key           = self.path.name
  override def persistenceId = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("{} has been stopped", key)

  override def receiveCommand: Receive = {
    case m: BPlusTreeEntity.RestoredIndex =>
      if (m.index.isEmpty) {
        buildIndex(self.toTyped[Indexing.IndexingProtocol], key, path)
        context.become(
          indexing(
            new scala.collection.mutable.HashSet[String]
          )
        )
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

  def indexing(
    unqWdsSet: scala.collection.mutable.HashSet[String]
  ): Receive = {
    case cmd: Indexing.IndexingProtocol =>
      cmd match {
        case Indexing.IndexingProtocol.Init(replyTo) =>
          replyTo.tell(Indexing.Confirm)

        case Indexing.IndexingProtocol.Next(replyTo, words) =>
          words.foreach(unqWdsSet.add(_))
          replyTo.tell(Indexing.Confirm)
          context.become(indexing(unqWdsSet))

        case Indexing.IndexingProtocol.OnCompleted =>
          val btree = new MemoryBPlusTree[String, Null](branchingFactor)
          try btree.insertKeys(unqWdsSet.toVector: _*)
          catch {
            case NonFatal(ex) =>
              log.warning(s"Failed to insertKeys" + ex.getMessage)
          }

          if (!unqWdsSet.isEmpty) {
            log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, unqWdsSet.size)
            saveSnapshot(UniqueTermsByShard2(unqWdsSet))
          }

          unstashAll()
          log.info("Became active {}:{}", key, unqWdsSet.size)
          context.become(active(btree))

        case Indexing.IndexingProtocol.Failure(ex) =>
          throw ex
      }

    case c =>
      log.info("{} is indexing right now. Stashing {} ...", key, c)
      stash()
  }

  // TODO: Add durable data ???
  def active(btree: MemoryBPlusTree[String, Null]): Receive = {
    case AddOneWord(word) =>
      persist(OneWordAdded(word)) { ev =>
        btree.insertKeys(word)
        context.become(active(btree))
      }

    case WordsQuery(prefix, maxResults, replyTo) =>
      val resBuf = Vector.newBuilder[String]

      val from = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)
      val to   = from + String.valueOf('z')

      val startTs = System.currentTimeMillis()
      val iter    = btree.boundedIterator((Symbol(">="), from), (Symbol("<"), to))
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
    val recoveredIndex = new MemoryBPlusTree[String, Null](branchingFactor)

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard2) =>
        recoveredIndex.insertKeys(snapshot.terms.toVector: _*)

      case OneWordAdded(w) =>
        recoveredIndex.insertKeys(w)

      case RecoveryCompleted =>
        val mb = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
        log.info(s"✅ Recovered $key. [${recoveredIndex.keys.size} terms, b-plus-tree size:$mb mb]")
        // 👍✅🚀🧪❌😄📣🔥🚨😱🥳
        self ! BPlusTreeEntity.RestoredIndex(recoveredIndex)
    }
  }
}
