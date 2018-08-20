package linguistic.serializers

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import linguistic.protocol._
import linguistic.serialization._

import scala.collection.immutable

class LinguisticsSerializer(val system: ExtendedActorSystem) extends SerializerWithStringManifest {
  override val identifier: Int = 99999

  override def manifest(obj: AnyRef): String = obj.getClass.getName

  override def toBinary(obj: AnyRef): Array[Byte] =
    obj match {
      case q: WordsQuery =>
        WordsQueryPB(q.keyword, q.maxResults).toByteArray
      case q: HomophonesQuery =>
        HomophonesQueryPB(q.keyword, q.maxResults).toByteArray
      case r: SearchResults =>
        SearchResultsPB(r.strict).toByteArray
      case h: Homophone =>
        HomophonePB(h.key, h.homophones).toByteArray
      case h: Homophones =>
        HomophonesPB(h.homophones.map(h => HomophonePB(h.key, h.homophones))).toByteArray
      case w: Words =>
        WordsPB(w.entry).toByteArray
      case _ =>
        throw new IllegalStateException(s"Serialization for $obj not supported. Check toBinary in ${this.getClass.getName}.")
    }


  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    if (manifest == classOf[WordsQuery].getName) {
      val pb = WordsQueryPB.parseFrom(bytes)
      WordsQuery(pb.keyword, pb.maxResults)
    } else if (manifest == classOf[HomophonesQuery].getName) {
      val pb = HomophonesQueryPB.parseFrom(bytes)
      HomophonesQuery(pb.keyword, pb.maxResults)
    } else if (manifest == classOf[SearchResults].getName) {
      val r = SearchResultsPB.parseFrom(bytes)
      SearchResults(r.strict.to[immutable.Seq])
    }else if (manifest == classOf[Homophone].getName) {
      val pb = HomophonePB.parseFrom(bytes)
      Homophone(pb.key, pb.homophones)
    } else if (manifest == classOf[Homophones].getName) {
      val pb = HomophonesPB.parseFrom(bytes)
      pb.homophones.map(h => Homophone(h.key, h.homophones))
    } else if (manifest == classOf[Words].getName) {
      val pb = WordsPB.parseFrom(bytes)
      Words(pb.entry)
    }
    else throw new IllegalStateException(
      s"Deserialization for $manifest not supported. Check fromBinary method in ${this.getClass.getName} class.")
}
