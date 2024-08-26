package linguistic.ps.cassandraBPlusTree

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import linguistic.protocol.SearchQuery.WordsQuery

import java.io.File
import linguistic.protocol.{AddOneWord, OneWordAdded, SearchQuery, SearchResults, UniqueTermsByShard2}
import linguistic.ps.Indexing

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.{Comparator, Locale}
import scala.util.control.NonFatal
import linguistic.ps.cassandraBPlusTree.BPlusEntity.mbDivider

import scala.collection.mutable

/*

https://github.com/scylladb/scylla-tools-java/blob/0b4accdd5ecb69a6346151987ba974e6be02b123/src/java/org/apache/cassandra/utils/btree/BTree.java#L38
https://github.com/scylladb/scylla-tools-java/blob/0b4accdd5ecb69a6346151987ba974e6be02b123/src/java/org/apache/cassandra/utils/MerkleTree.java
 */
object BPlusEntity {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat

  // (maximum number of branches in an internal node)
  val branchingFactor = 16

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(bPlusTreeIndex: Array[AnyRef])

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
    Props(new BPlusEntity()).withDispatcher("shard-dispatcher")
}

class BPlusEntity extends PersistentActor with ActorLogging with Indexing[Unit] with Stash {
  // val path = "./words.txt"
  val path = "./terms.txt"
  // val path  = "./list_of_english_words.txt"

  override def key           = self.path.name
  override def persistenceId = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start {} from:{} exists:{}", key, file.getAbsolutePath, file.exists)

    // self ! AddOneWord("xxxx")
  }

  override def postStop(): Unit =
    log.info("{} has been stopped", key)

  override def receiveCommand: Receive = {
    case m: BPlusEntity.RestoredIndex =>
      val size = BTree.size(m.bPlusTreeIndex)
      if (size == 0) {
        buildIndex(self.toTyped[Indexing.IndexingProtocol], key, path)

        /*val noOp = new UpdateFunction[Integer, Integer]() {
          override def apply(replacing: Integer, update: Integer): Integer = update
          override def abortEarly(): Boolean                               = false
          override def allocated(heapSize: Long): Unit                     = {}
          override def apply(i: Integer): Integer                          = i
        }

        val noOpStr = new UpdateFunction[String, String]() {
          override def apply(replacing: String, update: String): String = update
          override def abortEarly(): Boolean                            = false
          override def allocated(heapSize: Long): Unit                  = {}
          override def apply(i: String): String                         = i
        }

        val keys = java.util.Arrays.asList[Integer](10, 3, 2, 5, 4, 6, 7, 8, 9)

        val keys1                = java.util.Arrays.asList[Integer](1, 2, 4, 5, 6, 7, 8, 9, 11)
        val bTree: Array[AnyRef] = BTree.build(keys, noOp)
        BTree.apply[Integer](bTree, { i: Integer => println(i) }, true)

        val it = BTree.iterator(bTree, BTree.Dir.ASC)
        while (it.hasNext)
          println(it.next())

        val cmp    = Comparator.naturalOrder[Integer]()
        val cmpStr = Comparator.naturalOrder[String]()

        BTree.find[Integer](bTree, cmp, 11)
        BTree.isWellFormed(bTree, cmp)

        // aa, ab, aba, abcc, ac, dd, df, sdfs, vb
        val keysStr = java.util.Arrays.asList[String]("aa", "ab", "aba", "abcc", "ac", "dd", "df", "sdfs", "vb")
        java.util.Collections.sort[String](keysStr, cmpStr)
        val builder: BTree.Builder[String] = BTree.builder(cmpStr)
        builder.addAll(keysStr)
        builder.build()
         */

        context.become(indexing(new mutable.TreeSet[String]()))
      } else {
        unstashAll()
        val sizeInMb = BTree.sizeOfStructureOnHeap(m.bPlusTreeIndex).toFloat / mbDivider
        log.info(s"🔥 Index has been recovered from snapshot $key. [Size:${size}/SizeOfStructureOnHeap: $sizeInMb mb]")
        context.become(active(m.bPlusTreeIndex))
      }
    case c =>
      log.info(s"Stash: $c")
      stash()
  }

  def indexing(
    sortedWords: mutable.TreeSet[String]
  ): Receive = {
    case cmd: Indexing.IndexingProtocol =>
      cmd match {
        case Indexing.IndexingProtocol.Init(replyTo) =>
          replyTo.tell(Indexing.Confirm)

        case Indexing.IndexingProtocol.Next(replyTo, words) =>
          words.foreach(sortedWords.add(_))
          replyTo.tell(Indexing.Confirm)
          context.become(indexing(sortedWords))

        case Indexing.IndexingProtocol.OnCompleted =>
          var btree: Array[AnyRef] = Array.empty
          try {
            val b: BTree.Builder[String] = BTree.builder(Comparator.naturalOrder[String]())
            sortedWords.foreach(b.add(_))
            // b.addAll(sortedWords)
            btree = b.build()
          } catch {
            case NonFatal(ex) =>
              log.warning(s"Failed to insertKeys" + ex.getMessage)
          }

          if (!sortedWords.isEmpty) {
            log.info("IndexingCompleted for key [{}] (entries: {}), create snapshot now...", key, sortedWords.size)
            saveSnapshot(UniqueTermsByShard2(sortedWords))
          }

          unstashAll()
          log.info("Became active {}:{}", key, sortedWords.size)
          context.become(active(btree))

        case Indexing.IndexingProtocol.Failure(ex) =>
          throw ex
      }

    case c =>
      log.info("{} is indexing right now. Stashing {} ...", key, c)
      stash()
  }

  def active(btree: Array[AnyRef]): Receive = {
    case AddOneWord(word) =>
      persist(OneWordAdded(word)) { ev =>
        val updated =
          BTree.merge(btree, BTree.singleton(word), Comparator.naturalOrder[String](), UpdateFunction.noOp())
        context.become(active(updated))
      }

    case WordsQuery(prefix, maxResults, replyTo) =>
      val resBuf = Vector.newBuilder[String]

      val from = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)
      val to   = from + String.valueOf('z')

      val startTs = System.nanoTime()
      val iter    = BTree.slice[String, String](btree, Comparator.naturalOrder[String](), from, to, BTree.Dir.ASC)

      val latency = (System.nanoTime() - startTs) / 1000

      var i = 0
      while (iter.hasNext() && i <= maxResults) {
        val elem = iter.next()
        resBuf.+=(elem)
        i = i + 1
      }

      val results = resBuf.result()
      log.info(s"Search[$prefix...$to]. $latency micros")

      replyTo.tell(SearchResults(results))
      context.become(active(btree))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr $key")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    val builder: BTree.Builder[String] = BTree.builder(Comparator.naturalOrder[String]())

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard2) =>
        snapshot.terms.foreach(builder.add(_))

      case OneWordAdded(w) =>
        builder.add(w)

      case RecoveryCompleted =>
        val btree    = builder.build()
        val sizeInMb = BTree.sizeOfStructureOnHeap(btree).toFloat / mbDivider
        log.info(s"✅ Recovered $key. [Size:${BTree.size(btree)}/SizeOfStructureOnHeap: $sizeInMb mb]")
        self ! BPlusEntity.RestoredIndex(btree)
    }
  }
}
