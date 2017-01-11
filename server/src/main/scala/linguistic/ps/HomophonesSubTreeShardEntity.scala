package linguistic.ps

import java.io.File
import java.net.URLDecoder
import java.util.Locale

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion._
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.stream.ActorMaterializer
import com.rklaehn.radixtree.RadixTree
import linguistic.WordsSearchProtocol.{IndexingCompleted, SearchHomophones, SearchResults, SearchWord}
import linguistic.ps.HomophonesSubTreeShardEntity.Homophones
import linguistic.ps.WordsListSubTreeShardEntity.RestoredIndex

import scala.concurrent.duration._

object HomophonesSubTreeShardEntity {
  case class Homophones(key: String, homophones: Array[String])


  val extractShardId: ExtractShardId = {
    case x: SearchHomophones => x.keyword.toLowerCase(Locale.ROOT).take(1)
  }

  val extractEntityId: ExtractEntityId = {
    case x: SearchHomophones => (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
  }

  val Name = "homophones"

  def props(mat: ActorMaterializer): Props =
    Props(new HomophonesSubTreeShardEntity("./homophones103.txt")(mat)).withDispatcher("shard-dispatcher")
}

//born,borne,bourn,bourne
class HomophonesSubTreeShardEntity(path: String)(implicit val mat: ActorMaterializer) extends PersistentActor
  with ActorLogging with Indexing[Array[String]] with Stash with Passivation {

  override val startingLetter = self.path.name
    //self.path.name
  val passivationTimeout = 15.minutes
  context.setReceiveTimeout(passivationTimeout)

  override def persistenceId = HomophonesSubTreeShardEntity.Name + "-" + self.path.parent.name + "-" + self.path.name

  override def preStart() = {
    log.info("Started Homophones Entity Actor for key [{}] from the file {}", startingLetter, new File(path).getAbsolutePath)
  }

  override def postStop() = log.info("{} has been stopped", self)

  var recoveredIndex = RadixTree.empty[String, Array[String]]
  override def receiveRecover: Receive = {
    case SnapshotOffer(meta, hs: collection.immutable.Seq[Homophones]@unchecked) =>
      recoveredIndex = RadixTree(hs.map(x => x.key -> x.homophones): _*)
      log.info("SnapshotOffer {}: count: {}", meta.sequenceNr, recoveredIndex.count)
    case RecoveryCompleted =>
      log.info("RecoveryCompleted count:{} for key: {}", recoveredIndex.count, startingLetter)
      self ! RestoredIndex(recoveredIndex)
      recoveredIndex = RadixTree.empty[String, Array[String]]
  }

  override def receiveCommand = indexing(RadixTree.empty[String, Array[String]])

  /*def loop(words: Array[String], index: SubTree, n: Int): SubTree =
    if (n < words.length) {
      val i = n + 1
      val word = words(n)
      val left = words.slice(0, n)
      val right = words.slice(i, words.length)
      loop(words, index.merge(RadixTree[String, Array[String]](word -> (left ++ right))), n + 1)
    } else index*/

  def indexing(index: SubTree): Receive = {
    case line: String =>
      val words = line.split(',')
      val word = words(0)
      val other = words.slice(1, words.length)
      (context become indexing(index.merge(RadixTree[String, Array[String]](word -> other))))

    case IndexingCompleted =>
      log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", startingLetter, index.count)
      val homophones = index.entries
        .map { case (k, others) => Homophones(k, others) }
        .to[collection.immutable.Seq]

      saveSnapshot(homophones)
      unstashAll()
      (context become passivate(active(index)))

    case m: RestoredIndex[Array[String]]@unchecked =>
      if (m.index.count == 0) buildIndex(startingLetter, path)
      else {
        unstashAll()
        log.info("Index has been recovered from snapshot with size {} for key [{}]", m.index.count, startingLetter)
        (context become passivate(active(m.index)))
      }

    case SearchWord(prefix, _) =>
      log.info("ShardEntity [{}] is indexing right now. Stashing request for key [{}]", startingLetter, prefix)
      stash()
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long) = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  def active(index: SubTree): Receive = {
    case SearchHomophones(prefix, maxResults) =>
      val decodedPrefix = URLDecoder.decode(prefix, encoding)
      val results = index.filterPrefix(decodedPrefix).entries.take(maxResults)
        .map { case (key, hs) => key + "\t" + hs.mkString("\t") }
        .to[collection.immutable.Seq]

      log.info("Search for homophones: [{}], resulted in [{}] results on [{}]", prefix, results.size, decodedPrefix)
      sender() ! SearchResults(results)
  }
}