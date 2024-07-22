package linguistic.ps

import akka.actor.{ActorLogging, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}

import java.io.File
import com.abahgat.suffixtree.GeneralizedSuffixTree
import linguistic.protocol.{AddOneWord, IndexingCompleted, OneWordAdded, SearchQuery, SearchResults, UniqueTermsByShard}
import linguistic.ps.RadixTreeShardEntity.mbDivider
import org.openjdk.jol.info.GraphLayout

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import scala.collection.mutable.Builder
import scala.util.control.NonFatal
import one.nio.util.Hex

/*
info] 17:19:55.383UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/q/q] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(q, 0, 1720286276635, None): count:577 0.031791687/0.70751953 mb
[info] 17:19:55.831UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/l/l] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(l, 0, 1720286266699, None): count:3363 0.18206787/3.077095 mb
[info] 17:19:55.836UTC |INFO | [linguistics-shard-dispatcher-30, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/n/n] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(n, 0, 1720286270670, None): count:2475 0.13777924/3.0852814 mb
[info] 17:19:55.859UTC |INFO | [linguistics-shard-dispatcher-27, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/e/e] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(e, 0, 1720286252664, None): count:4494 0.2514038/4.924713 mb
[info] 17:19:55.947UTC |INFO | [linguistics-shard-dispatcher-35, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/r/r] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(r, 0, 1720286278708, None): count:6804 0.37771606/6.8816757 mb
[info] 17:19:55.957UTC |INFO | [linguistics-shard-dispatcher-30, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/z/z] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(z, 0, 1720286294669, None): count:265 0.01424408/0.28823853 mb
[info] 17:19:55.966UTC |INFO | [linguistics-shard-dispatcher-33, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/p/p] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(p, 0, 1720286274736, None): count:8448 0.46878815/8.896477 mb
[info] 17:19:56.015UTC |INFO | [linguistics-shard-dispatcher-27, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/v/v] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(v, 0, 1720286286684, None): count:1825 0.1004715/1.8782501 mb
[info] 17:19:56.042UTC |INFO | [linguistics-shard-dispatcher-31, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/c/c] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(c, 0, 1720286248829, None): count:10324 0.57281494/10.690834 mb
[info] 17:19:56.079UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/h/h] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(h, 0, 1720286258677, None): count:3920 0.21504211/4.0601654 mb
[info] 17:19:56.080UTC |INFO | [linguistics-shard-dispatcher-31, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/x/x] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(x, 0, 1720286290675, None): count:79 0.004508972/0.1018219 mb
[info] 17:19:56.289UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/k/k] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(k, 0, 1720286264648, None): count:964 0.051651/0.99276733 mb
[info] 17:19:56.294UTC |INFO | [linguistics-shard-dispatcher-35, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/t/t] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(t, 0, 1720286282781, None): count:5530 0.30282593/5.4358597 mb
[info] 17:19:56.340UTC |INFO | [linguistics-shard-dispatcher-35, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/y/y] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(y, 0, 1720286292725, None): count:370 0.019592285/0.35168457 mb
[info] 17:19:56.435UTC |INFO | [linguistics-shard-dispatcher-30, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/d/d] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(d, 0, 1720286250720, None): count:6694 0.37142944/6.9577103 mb
[info] 17:19:56.489UTC |INFO | [linguistics-shard-dispatcher-31, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/m/m] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(m, 0, 1720286268840, None): count:5806 0.3206482/6.204445 mb
[info] 17:19:56.497UTC |INFO | [linguistics-shard-dispatcher-33, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/a/a] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(a, 0, 1720286245615, None): count:6541 0.36284637/6.857376 mb
[info] 17:19:56.699UTC |INFO | [linguistics-shard-dispatcher-27, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/s/s] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(s, 0, 1720286280857, None): count:12108 0.6668625/11.110359 mb
[info] 17:19:56.706UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/i/i] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(i, 0, 1720286260698, None): count:4382 0.24887848/5.5509033 mb
[info] 17:19:56.759UTC |INFO | [linguistics-shard-dispatcher-35, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/f/f] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(f, 0, 1720286254690, None): count:4701 0.25749207/4.377022 mb
[info] 17:19:56.842UTC |INFO | [linguistics-shard-dispatcher-35, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/j/j] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(j, 0, 1720286262647, None): count:1046 0.05622101/1.0325851 mb
[info] 17:19:56.848UTC |INFO | [linguistics-shard-dispatcher-31, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/w/w] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(w, 0, 1720286288742, None): count:2714 0.14683533/2.4414978 mb
[info] 17:19:56.892UTC |INFO | [linguistics-shard-dispatcher-33, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/u/u] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(u, 0, 1720286284727, None): count:3312 0.18764496/4.121689 mb
[info] 17:19:56.926UTC |INFO | [linguistics-shard-dispatcher-34, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/g/g] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(g, 0, 1720286256661, None): count:3594 0.19499207/3.403099 mb
[info] 17:19:56.926UTC |INFO | [linguistics-shard-dispatcher-27, linguistics, akka://linguistics@127.0.0.1:2551/system/sharding/words/o/o] linguistic.ps.SuffixTreeEntity - SnapshotOffer SnapshotMetadata(o, 0, 1720286272659, None): count:2966 0.16560364/3.5136108 mb
 */

object SuffixTreeEntity22 {

  val Name      = "words"
  val mbDivider = (1024 * 1024).toFloat //1_048_576

  final case class UniqueWords(entry: Seq[String]) extends AnyVal
  final case class RestoredIndex(
    index: GeneralizedSuffixTree,
    uniqueWords: one.nio.mem.LongObjectHashMap[Array[Byte]]
  )

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

  def props(isPrefixBasedSearch: Boolean): Props =
    Props(new SuffixTreeEntity22(isPrefixBasedSearch)).withDispatcher("shard-dispatcher")

}

class SuffixTreeEntity22(isPrefixBasedSearch: Boolean)
    extends PersistentActor
    with ActorLogging
    with Indexing[Unit]
    with Stash
    with Passivation {

  //val path = "./words.txt"
  val path = "./terms.txt"

  override val key = self.path.name

  override def persistenceId: String = key

  override def preStart(): Unit = {
    val file = new File(path)
    log.info("Start ShardEntity:{} from:{} exists:{}", key, file.getAbsolutePath, file.exists)
  }

  override def postStop(): Unit =
    log.info("ShardEntity:{} has been stopped", key)

  override def receiveCommand: Receive =
    indexing(new GeneralizedSuffixTree(), 0, new one.nio.mem.LongObjectHashMap[Array[Byte]](1000000 / 2))

  def indexing(index: GeneralizedSuffixTree, i: Int, vb: one.nio.mem.LongObjectHashMap[Array[Byte]]): Receive = {
    case word: String =>
      if (ThreadLocalRandom.current().nextDouble() > .99995)
        println(s"$i: $word")

      try {
        index.put(word, i)
        vb.put(i.toLong, word.getBytes)
        context.become(indexing(index, i + 1, vb))
      } catch {
        case NonFatal(ex) =>
          println(s"Failed to add $word in pos: ${i + 1}")
          context.become(indexing(index, i, vb))
      }

    case IndexingCompleted =>
      unstashAll()
      context become active(index, vb)

    case m: SuffixTreeEntity22.RestoredIndex =>
      val cnt = m.uniqueWords.size
      if (cnt == 0) buildIndex(???, key, path)
      else {
        unstashAll()
        /*
        val mb  = GraphLayout.parseInstance(m.index).totalSize.toFloat / mbDivider
        val mb1 = GraphLayout.parseInstance(m.uniqueWords).totalSize.toFloat / mbDivider
        log.info(s"Index has been recovered from snapshot with size ${cnt} [${mb}/${mb1}] for key=$key")
         */
        log.info(s"Index has been recovered from snapshot with $cnt terms for key=$key")

        //off-heap lock-free hash tables with 64-bit keys.
        /*
        import one.nio.util.Hex
        val map = new one.nio.mem.LongObjectHashMap[Array[Byte]](5) //5 kv only possible
        map.put(0L, Hex.parseBytes("1111aaaa"))
        map.put(1L, Hex.parseBytes("11111bbb"))
         */

        context become active(m.index, m.uniqueWords)
      }

    //case SearchQuery.WordsQuery(prefix, _) | case cmd: AddOneWord ⇒
    case _ =>
      log.info("{} is indexing right now. stashing requests ", key)
      stash()
  }

  def active(
    index: GeneralizedSuffixTree,
    shardUniqueWords: one.nio.mem.LongObjectHashMap[Array[Byte]] /*Vector[String]*/
  ): Receive = {
    case AddOneWord(w) =>
      persist(OneWordAdded(w)) { ev =>
        /*
        val updatedShardUniqueWords = shardUniqueWords.:+(w)
        index.put(w, updatedShardUniqueWords.size - 1)
        context.become(active(index, updatedShardUniqueWords))
         */
      }

    case SearchQuery.WordsQuery(prefix, maxResultSize, replyTo) =>
      val decodedPrefix = URLDecoder.decode(prefix, StandardCharsets.UTF_8.name)
      val startTs       = System.nanoTime

      var buf = Vector.newBuilder[String]
      val it  = index.search(decodedPrefix).iterator()

      var i = 0
      while (it.hasNext() && i < maxResultSize) {
        val ind       = it.next()
        val candidate = new String(shardUniqueWords.get(ind.toLong))
        if (isPrefixBasedSearch)
          if (candidate.startsWith(prefix)) {
            buf.+=(candidate)
            i = i + 1
          } else
            log.info(s"Search by {}: ignore {}", prefix, candidate)
        else {
          buf.+=(candidate)
          i = i + 1
        }
      }

      val searchResult = buf.result()
      log.info(
        "Search for: [{}], resulted in [{}] results. {} millis",
        prefix,
        searchResult.size,
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime - startTs)
      )

      replyTo.tell(SearchResults(searchResult))
  }

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error(cause, s"Persistence error on: $seqNr {}")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveRecover: Receive = {
    val recoveredIndex = new GeneralizedSuffixTree()
    //var recoveredWords: Vector[String] = Vector.empty
    var map: one.nio.mem.LongObjectHashMap[Array[Byte]] = new one.nio.mem.LongObjectHashMap[Array[Byte]](1000000 / 2)

    {
      case SnapshotOffer(meta, snapshot: UniqueTermsByShard) =>
        val startTs = System.currentTimeMillis()
        map = new one.nio.mem.LongObjectHashMap[Array[Byte]](snapshot.terms.size)

        snapshot.terms.zipWithIndex.foreach { case (w, i) =>
          try {
            recoveredIndex.put(w, i)
            map.put(i, w.getBytes)
          } catch {
            case NonFatal(ex) =>
              //ex.printStackTrace()
              println(s"Failed to recover $w in pos: ${i + 1}")
          }
        }
        //recoveredWords = snapshot.terms.toVector

        val lat = (System.currentTimeMillis() - startTs) / 1000
        // SnapshotOffer SnapshotMetadata(a, 0, 1720457214156, None): count:466263 Latency:32 sec
        log.info(s"SnapshotOffer $meta: ${snapshot.terms.length} terms. Latency:${lat}sec")

      case OneWordAdded(w) =>
      //recoveredWords = recoveredWords.:+(w)
      //recoveredIndex.put(w, recoveredWords.length - 1)
      /*
        map.put(map.size() -1, w.getBytes())
        recoveredIndex.put(w, map.size() - 1)
       */

      case RecoveryCompleted =>
        log.info("Recovered index by key: {} count:{}", key, recoveredIndex.computeCount)
        self ! SuffixTreeEntity22.RestoredIndex(recoveredIndex, map)

    }
  }
}
