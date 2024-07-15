package linguistic.ps

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion._
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.rklaehn.radixtree.RadixTree
import linguistic.protocol.SearchQuery.WordsQuery
import linguistic.protocol.{AddOneWord, IndexingCompleted, OneWordAdded, SearchResults, UniqueTermsByShard}
import linguistic.ps.RadixTreeShardEntity._
import org.openjdk.jol.info.GraphLayout

object RadixTreeShardEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat //1_048_576

  final case class RestoredIndex[T](index: RadixTree[String, T]) extends AnyVal

  // shards: [a,...,z]
  val extractShardId: ExtractShardId = {
    case x: WordsQuery =>
      x.keyword.toLowerCase(Locale.ROOT).take(1)
    case x: AddOneWord ⇒
      x.w.toLowerCase(Locale.ROOT).take(1)
    case ShardRegion.StartEntity(id) =>
      id
  }

  // entity: [a,...,z]
  val extractEntityId: ExtractEntityId = {
    case x: AddOneWord ⇒
      (x.w.toLowerCase(Locale.ROOT).take(1), x)
    case x: WordsQuery =>
      (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
  }

  def props(): Props =
    Props(new RadixTreeShardEntity()).withDispatcher("shard-dispatcher")
}

/**
  *
  * Each ShardEntity actor uses RadixTree to store his state.
  * Filtering by prefix is extremely fast with a radix tree (worst case O(log(N)),
  * whereas it is worse than O(N) with SortedMap and HashMap.
  * Filtering by prefix will also benefit a lot from structural sharing.
  *
  */
class RadixTreeShardEntity extends PersistentActor with ActorLogging with Indexing[Unit]
    with Stash with Passivation {

  val path = "./words.txt"

  override val key = self.path.name

  //context.setReceiveTimeout(passivationTimeout)

  override def persistenceId = key

  override def preStart() = {
    val file = new File(path)
    log.info("Start ShardEntity:{} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop() =
    log.info("ShardEntity:{} has been stopped", key)

  override def receiveCommand =
    indexing(RadixTree.empty[String, Unit])

  def indexing(index: SubTree): Receive = {
    case word: String =>
      context.become(indexing(index merge RadixTree[String, Unit](word -> ())))

    case IndexingCompleted =>
      val words = index.keys.to[collection.immutable.Seq]

      if (words.size > 0) {
        log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, index.count)
        saveSnapshot(UniqueTermsByShard(words))
      }

      unstashAll()
      context become passivate(searchable(index))

    case m: RestoredIndex[Unit] @unchecked =>
      if (m.index.count == 0) buildIndex(???, key, path)
      else {
        unstashAll()
        log.info("Index has been recovered from snapshot with size {} for key [{}]", m.index.count, key)
        context become passivate(searchable(m.index))
      }

    case WordsQuery(prefix, _, _) =>
      log.info("ShardEntity [{}] is indexing right now. Stashing request for key [{}]", key, prefix)
      stash()
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long) = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    var recoveredIndex = RadixTree.empty[String, Unit]

    {
      case SnapshotOffer(meta, w: UniqueTermsByShard) =>
        recoveredIndex = RadixTree(w.terms.map(name => name -> ()): _*)
        //log.info("SnapshotOffer {}: count: {}", meta.sequenceNr, recoveredIndex.count)
        val mb = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
        log.info("SnapshotOffer {}: count:{}", meta, recoveredIndex.count)

      case OneWordAdded(word) =>
        recoveredIndex = recoveredIndex.merge(RadixTree[String, Unit](word -> ()))

      case RecoveryCompleted =>
        log.info("Recovered index by key: {} count:{}", key, recoveredIndex.count)
        self ! RestoredIndex(recoveredIndex)
        recoveredIndex = RadixTree.empty[String, Unit]
    }
  }

  def searchable(index: SubTree): Receive = {
    case AddOneWord(word) =>
      persist(OneWordAdded(word)) { ev =>
        val updatedIndex = index.merge(RadixTree[String, Unit](word -> ()))
        context.become(searchable(updatedIndex))
      }

    case WordsQuery(prefix, maxResults, replyTo) =>
      val decodedPrefix = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)
      val start         = System.nanoTime
      val results       = index.filterPrefix(decodedPrefix).keys.take(maxResults).to[collection.immutable.Seq]
      log.info(
        "Search for: [{}], resulted in [{}] results. Latency:{} millis",
        prefix,
        results.size,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime - start)
      )
      replyTo.tell(SearchResults(results))
  }
}
