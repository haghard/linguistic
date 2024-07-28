package linguistic.ps

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import linguistic.protocol.SearchQuery.WordsQuery

import java.io.File
import linguistic.protocol.{AddOneWord, IndexingCompleted, OneWordAdded, SearchQuery, SearchResults, UniqueTermsByShard}
import linguistic.pruningRadixTrie.JPruningRadixTrie
import linguistic.ps.PruningRadixTrieEntity2.mbDivider
import org.openjdk.jol.info.GraphLayout

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

object PruningRadixTrieEntity2 {

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
    Props(new PruningRadixTrieEntity2()).withDispatcher("shard-dispatcher")
}

class PruningRadixTrieEntity2
    extends PersistentActor
    with ActorLogging
    with Indexing[Unit]
    with Stash
    with Passivation {

  // val path             = "./words.txt"
  val path             = "./terms.txt"
  val snapshotFilePath = s"./PruningRadixTrie/$key.txt"

  override def key           = self.path.name
  override def persistenceId = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("PruningRadixTrieEntity({}) has been stopped", key)

  override def receiveCommand: Receive =
    indexing(new JPruningRadixTrie())

  def indexing(index: JPruningRadixTrie): Receive = {
    case term: String =>
      index.addTerm(term, 1)
      context.become(indexing(index))

    case IndexingCompleted =>
      if (index.termCount > 0) {
        log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, index.termCount)
        // saveSnapshot(UniqueTermsByShard(words))
        index.writeTermsToFile(snapshotFilePath)
      }
      unstashAll()
      context become passivate(active(index))

    case m: PruningRadixTrieEntity2.RestoredIndex =>
      val cnt = m.index.termCount
      if (cnt == 0) buildIndex(???, key, path)
      else {
        unstashAll()
        log.info(s"Index has been recovered from snapshot with size ${cnt}] for key=$key")
        context become passivate(active(m.index))
      }

    case _ =>
      log.info("{} is indexing right now. stashing requests ", key)
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
      val startTs       = System.nanoTime

      val iter  = index.getTopkTermsForPrefix(decodedPrefix, maxResults).iterator()
      val endTs = System.nanoTime

      var buf = Vector.newBuilder[String]
      while (iter.hasNext) {
        val term = iter.next()
        buf.+=(term.getTerm)
      }
      val results = buf.result()

      log.info(
        "Search for: [{}], resulted in [{}] results. {} millis",
        prefix,
        results.size,
        TimeUnit.NANOSECONDS.toMillis(endTs - startTs)
      )
      replyTo.tell(SearchResults(results))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr $key")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    var recoveredIndex = new JPruningRadixTrie()

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard) =>

      case OneWordAdded(w) =>
        recoveredIndex.addTerm(w, 1)

      case RecoveryCompleted =>
        val index = new JPruningRadixTrie()
        index.readTermsFromFile(snapshotFilePath)
        recoveredIndex = index
        val mb = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
        log.info("Recovered index by key: {} count:{} {}mb", key, recoveredIndex.termCount, mb)
        self ! PruningRadixTrieEntity2.RestoredIndex(recoveredIndex)
    }
  }
}
