package linguistic.serializers

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRefResolver
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.serialization.SerializerWithStringManifest
import linguistic.protocol.SearchQuery
import linguistic.protocol._
import linguistic.ps.SuffixTreeEntity.mbDivider
import linguistic.serialization._

import scala.collection.immutable

class LinguisticsSerializer(val system: ExtendedActorSystem) extends SerializerWithStringManifest {
  override val identifier: Int = 99999

  val refResolver = ActorRefResolver(system.toTyped)

  override def manifest(obj: AnyRef): String = obj.getClass.getName

  override def toBinary(obj: AnyRef): Array[Byte] =
    obj match {
      case q: SearchQuery.WordsQuery =>
        WordsQueryPB(q.keyword, q.maxResults, refResolver.toSerializationFormat(q.replyTo)).toByteArray
      case q: SearchQuery.HomophonesQuery =>
        HomophonesQueryPB(q.keyword, q.maxResults, refResolver.toSerializationFormat(q.replyTo)).toByteArray
      case r: SearchResults =>
        SearchResultsPB(r.strict).toByteArray
      case h: Homophone =>
        HomophonePB(h.key, h.homophones).toByteArray
      case h: Homophones =>
        HomophonesPB(h.homophones.map(h => HomophonePB(h.key, h.homophones))).toByteArray
      case snapshot: UniqueTermsByShard =>
        val pb        = WordsPB(snapshot.terms)
        val shardName = snapshot.terms.headOption.map(_.head.toString).getOrElse("")
        val mb        = pb.serializedSize.toFloat / mbDivider
        system.log.info("-- Snapshot:Save({}) {}mb", shardName, mb)
        pb.toByteArray
      case snapshot: UniqueTermsByShard2 =>
        val pb        = WordsPB(snapshot.terms.toVector)
        val shardName = snapshot.terms.headOption.map(_.head.toString).getOrElse("")
        val mb        = pb.serializedSize.toFloat / mbDivider
        // "👍✅🚀🧪❌😄📣🔥🚨😱🥳❤️,😄,😐,😞"
        system.log.info("👍 Snapshot:Save({}) {}mb", shardName, mb)
        pb.toByteArray
      // cmd
      case AddOneWord(w) =>
        AddOneWordPB(w).toByteArray

      // ev
      case OneWordAdded(w) =>
        OneWordAddedPB(w).toByteArray
      case _ =>
        throw new IllegalStateException(
          s"Serialization for $obj not supported. Check toBinary in ${this.getClass.getName}."
        )
    }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (manifest == classOf[SearchQuery.WordsQuery].getName) {
      val pb = WordsQueryPB.parseFrom(bytes)
      SearchQuery.WordsQuery(pb.keyword, pb.maxResults, refResolver.resolveActorRef[SearchResults](pb.replyTo))
    } else if (manifest == classOf[SearchQuery.HomophonesQuery].getName) {
      val pb = HomophonesQueryPB.parseFrom(bytes)
      SearchQuery.HomophonesQuery(pb.keyword, pb.maxResults, refResolver.resolveActorRef[SearchResults](pb.replyTo))
    } else if (manifest == classOf[SearchResults].getName) {
      val r = SearchResultsPB.parseFrom(bytes)
      SearchResults(r.strict.to[immutable.Seq])
    } else if (manifest == classOf[Homophone].getName) {
      val pb = HomophonePB.parseFrom(bytes)
      Homophone(pb.key, pb.homophones)
    } else if (manifest == classOf[Homophones].getName) {
      val pb = HomophonesPB.parseFrom(bytes)
      pb.homophones.map(h => Homophone(h.key, h.homophones))
    } else if (manifest == classOf[UniqueTermsByShard].getName) {
      val pb        = WordsPB.parseFrom(bytes)
      val shardName = pb.entry.headOption.map(_.head.toString).getOrElse("")
      // val mb    = GraphLayout.parseInstance(snapshot.words).totalSize.toFloat / mbDivider
      val mb = pb.serializedSize.toFloat / mbDivider
      system.log.info("-- Snapshot:Read({}) {}mb", shardName, mb)
      UniqueTermsByShard(pb.entry)
    } else if (manifest == classOf[UniqueTermsByShard2].getName) {
      val pb        = WordsPB.parseFrom(bytes)
      val shardName = pb.entry.headOption.map(_.head.toString).getOrElse("")
      val mb        = pb.serializedSize.toFloat / mbDivider
      system.log.info("🚀 Snapshot:Read({}) {}mb", shardName, mb)
      UniqueTermsByShard2(scala.collection.mutable.TreeSet(pb.entry: _*))
    } else if (manifest == classOf[AddOneWord].getName) {
      AddOneWord(AddOneWordPB.parseFrom(bytes).word)
    } else if (manifest == classOf[OneWordAdded].getName) {
      OneWordAdded(OneWordAddedPB.parseFrom(bytes).word)
    } else
      throw new IllegalStateException(
        s"Deserialization for $manifest not supported. Check fromBinary method in ${this.getClass.getName} class."
      )
}
