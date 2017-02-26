package linguistic.ps

import java.io.File
import java.net.URLDecoder
import java.util.Locale

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion._
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.stream.ActorMaterializer
import com.rklaehn.radixtree.RadixTree
import linguistic.WordsSearchProtocol.{IndexingCompleted, SearchResults, SearchWord}
import linguistic.ps.WordShardEntity._

import scala.concurrent.duration._

object WordShardEntity {
  val mbDivider = (1024 * 1024).toFloat

  case class RestoredIndex[T](index: RadixTree[String, T])

  val extractShardId: ExtractShardId = {
    case x: SearchWord => x.keyword.toLowerCase(Locale.ROOT).take(1)
  }

  val extractEntityId: ExtractEntityId = {
    case x: SearchWord => (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
  }

  val Name = "wordslist"

  def props(mat: ActorMaterializer): Props =
    Props(new WordShardEntity("./words.txt")(mat)).withDispatcher("shard-dispatcher")
}

/**
  *
  * Each ShardEntity actor uses RadixTree to store his state.
  * Filtering by prefix is extremely fast with a radix tree (worst case O(log(N)),
  * whereas it is worse than O(N) with SortedMap and HashMap.
  * Filtering by prefix will also benefit a lot from structural sharing.
  *
  */
class WordShardEntity(path: String)(implicit val mat: ActorMaterializer) extends PersistentActor
  with ActorLogging with Indexing[Unit] with Stash with Passivation {

  override val key = self.path.name

  //self.path.name
  val passivationTimeout = 15.minutes
  context.setReceiveTimeout(passivationTimeout)

  override def persistenceId = key

  override def preStart() = {
    val file = new File(path)
    log.info("Started Entity Actor for key [{}] file.exists: {}", key, file.exists())
  }

  override def postStop() = log.info("{} has been stopped", self)

  override def receiveCommand = indexing(RadixTree.empty[String, Unit])

  def indexing(index: SubTree): Receive = {
    case word: String =>
      context.become(indexing(index merge RadixTree[String, Unit](word ->())))

    case IndexingCompleted =>
      log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, index.count)
      val indSeq = index.keys.to[collection.immutable.Seq]
      saveSnapshot(indSeq)
      unstashAll()
      (context become passivate(active(index)))

    case m: RestoredIndex[Unit]@unchecked =>
      if (m.index.count == 0) buildIndex(key, path)
      else {
        unstashAll()
        log.info("Index has been recovered from snapshot with size {} for key [{}]", m.index.count, key)
        (context become passivate(active(m.index)))
      }

    case SearchWord(prefix, _) =>
      log.info("ShardEntity [{}] is indexing right now. Stashing request for key [{}]", key, prefix)
      stash()
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long) = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  var recoveredIndex = RadixTree.empty[String, Unit]

  override def receiveRecover = {
    case SnapshotOffer(meta, seq: collection.immutable.Seq[String]@unchecked) =>
      recoveredIndex = RadixTree(seq.map(name => name -> (())): _*)
      //val mb = GraphLayout.parseInstance(recoveredIndex).totalSize.toFloat / mbDivider
      //log.info("SnapshotOffer {}: count:{}", meta, recoveredIndex.count)
    case RecoveryCompleted =>
      log.info("Recovered key: {} count:{}", key, recoveredIndex.count)
      self ! RestoredIndex(recoveredIndex)
      recoveredIndex = RadixTree.empty[String, Unit]
  }

  def active(index: SubTree): Receive = {
    case SearchWord(prefix, maxResults) =>
      val decodedKeyword = URLDecoder.decode(prefix, encoding)
      val results = index.filterPrefix(decodedKeyword).keys.take(maxResults).to[collection.immutable.Seq]
      log.info("Search for: [{}], resulted in [{}] results on [{}]", prefix, results.size, decodedKeyword)
      sender() ! SearchResults(results)
  }
}