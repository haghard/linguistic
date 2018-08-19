package linguistic.ps

import java.io.File
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion._
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.stream.ActorMaterializer
import com.rklaehn.radixtree.RadixTree
import linguistic.WordsSearchProtocol.{IndexingCompleted, SearchResults}
import linguistic.protocol.{Words, WordsQuery}
import linguistic.ps.WordShardEntity._

object WordShardEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat

  case class RestoredIndex[T](index: RadixTree[String, T])

  val extractShardId: ExtractShardId = {
    case x: WordsQuery =>
      x.keyword.toLowerCase(Locale.ROOT).take(1)
    case ShardRegion.StartEntity(id) =>
      id
  }

  val extractEntityId: ExtractEntityId = {
    case x: WordsQuery =>
      (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
  }

  def props(mat: ActorMaterializer): Props =
    Props(new WordShardEntity()(mat)).withDispatcher("shard-dispatcher")
}

/**
  *
  * Each ShardEntity actor uses RadixTree to store his state.
  * Filtering by prefix is extremely fast with a radix tree (worst case O(log(N)),
  * whereas it is worse than O(N) with SortedMap and HashMap.
  * Filtering by prefix will also benefit a lot from structural sharing.
  *
  */
class WordShardEntity(implicit val mat: ActorMaterializer) extends PersistentActor
  with ActorLogging with Indexing[Unit] with Stash with Passivation {

  val path = "./words.txt"

  override val key = self.path.name

  context.setReceiveTimeout(passivationTimeout)

  override def persistenceId = key

  override def preStart() = {
    val file = new File(path)
    log.info("Start ShardEntity:[{}] from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop() =
    log.info("ShardEntity has been stopped")

  override def receiveCommand =
    indexing(RadixTree.empty[String, Unit])

  def indexing(index: SubTree): Receive = {
    case word: String =>
      context.become(indexing(index merge RadixTree[String, Unit](word -> ())))

    case IndexingCompleted =>
      log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, index.count)
      val words = index.keys.to[collection.immutable.Seq]

      if(words.size > 0)
        saveSnapshot(Words(words))

      unstashAll()
      context become passivate(searchable(index))

    case m: RestoredIndex[Unit]@unchecked =>
      if (m.index.count == 0) buildIndex(key, path)
      else {
        unstashAll()
        log.info("Index has been recovered from snapshot with size {} for key [{}]", m.index.count, key)
        context become passivate(searchable(m.index))
      }

    case WordsQuery(prefix, _) =>
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
      case SnapshotOffer(meta, w: Words) =>
        recoveredIndex = RadixTree(w.entry.map(name => name -> (())): _*)
        log.info("SnapshotOffer {}: count: {}", meta.sequenceNr, recoveredIndex.count)
      //val mb = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
      //log.info("SnapshotOffer {}: count:{}", meta, recoveredIndex.count)
      case RecoveryCompleted =>
        log.info("Recovered index by key: {} count:{}", key, recoveredIndex.count)
        self ! RestoredIndex(recoveredIndex)
        recoveredIndex = RadixTree.empty[String, Unit]
    }
  }

  def searchable(index: SubTree): Receive = {
    case WordsQuery(prefix, maxResults) =>
      val decodedPrefix = URLDecoder.decode(prefix, encoding)
      val start = System.nanoTime
      val results = index.filterPrefix(decodedPrefix).keys.take(maxResults).to[collection.immutable.Seq]
      log.info("Search for: [{}], resulted in [{}] results. Latency:{} millis", prefix, results.size,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime - start))
      sender() ! SearchResults(results)
  }
}