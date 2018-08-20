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
import linguistic.protocol._
import linguistic.ps.WordShardEntity.RestoredIndex

object HomophonesSubTreeShardEntity {
  val Name = "homophones"

  val extractShardId: ExtractShardId = {
    case x: HomophonesQuery =>
      x.keyword.toLowerCase(Locale.ROOT).take(1)
    case ShardRegion.StartEntity(id) =>
      id
  }

  val extractEntityId: ExtractEntityId = {
    case x: HomophonesQuery =>
      (x.keyword.toLowerCase(Locale.ROOT).take(1), x)
  }

  def props(mat: ActorMaterializer): Props =
    Props(new HomophonesSubTreeShardEntity()(mat)).withDispatcher("shard-dispatcher")
}

class HomophonesSubTreeShardEntity(implicit val mat: ActorMaterializer) extends PersistentActor
  with ActorLogging with Indexing[Seq[String]] with Stash with Passivation {

  val path = "./homophones.txt"

  override val key = self.path.name

  context.setReceiveTimeout(passivationTimeout)

  override def persistenceId =
    HomophonesSubTreeShardEntity.Name + "-" + self.path.parent.name + "-" + self.path.name

  override def preStart() = {
    val file = new File(path)
    log.info("Start ShardEntity:[{}] from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop() =
    log.info("{} has been stopped", self)

  override def receiveRecover: Receive = {
    var recoveredIndex = RadixTree.empty[String, Seq[String]]

    {
      case SnapshotOffer(meta, hs: Homophones) =>
        recoveredIndex = RadixTree(hs.homophones.map(x => x.key -> x.homophones): _*)
        log.info("SnapshotOffer {}: count: {}", meta.sequenceNr, recoveredIndex.count)
      case RecoveryCompleted =>
        log.info("RecoveryCompleted count:{} for key: {}", recoveredIndex.count, key)
        self ! RestoredIndex(recoveredIndex)
    }
  }

  override def receiveCommand =
    indexing(RadixTree.empty[String, Seq[String]])

  def indexing(index: SubTree): Receive = {
    case line: String =>
      val words = line.split(',')
      val word = words(0)
      val other = words.slice(1, words.length)
      context become indexing(index.merge(RadixTree[String, Seq[String]](word -> other)))

    case IndexingCompleted =>
      log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, index.count)
      val homophones = index.entries
        .map { case (k, others) => Homophone(k, others) }
        .to[collection.immutable.Seq]

      if (homophones.size > 0)
        saveSnapshot(Homophones(homophones))

      unstashAll()
      context become passivate(searchable(index))

    case m: RestoredIndex[Seq[String]]@unchecked =>
      if (m.index.count == 0) buildIndex(key, path)
      else {
        unstashAll()
        log.info("Index has been recovered from snapshot with size {} for key [{}]", m.index.count, key)
        context become passivate(searchable(m.index))
      }

    case HomophonesQuery(prefix, _) =>
      log.info("ShardEntity [{}] is indexing right now. Stashing request for key [{}]", key, prefix)
      stash()
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long) = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  def searchable(index: SubTree): Receive = {
    case HomophonesQuery(prefix, maxResults) =>
      val decodedPrefix = URLDecoder.decode(prefix, encoding)
      val results =
        index
          .filterPrefix(decodedPrefix).entries.take(maxResults)
          .map { case (key, hs) => s"$key\t${hs.mkString("\t")}" }
          .to[collection.immutable.Seq]

      val start = System.nanoTime
      log.info("Search for homophones: [{}] resulted in [{}] results. Latency: {} millis", prefix,
        results.size, TimeUnit.NANOSECONDS.toMillis(System.nanoTime - start))
      sender() ! SearchResults(results)
  }
}


/*
def loop(words: Array[String], index: SubTree, n: Int): SubTree =
  if (n < words.length) {
    val i = n + 1
    val word = words(n)
    val left = words.slice(0, n)
    val right = words.slice(i, words.length)
    loop(words, index.merge(RadixTree[String, Array[String]](word -> (left ++ right))), n + 1)
  } else index
*/