package linguistic.ps

import java.io.{File, FileInputStream}
import java.util.Locale

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.StreamConverters.fromInputStream
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.ByteString
import com.rklaehn.radixtree.RadixTree
import linguistic.protocol.IndexingCompleted

import scala.concurrent.Future

trait Indexing[T] {
  mixin: PersistentActor with ActorLogging =>

  implicit def mat: ActorMaterializer

  type SubTree = RadixTree[String, T]

  def key: String

  def keywordsSource(path: String): Source[ByteString, Future[IOResult]] =
    fromInputStream(() => new FileInputStream(new File(path)))
      .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))

  def buildIndex(key: String, path: String): Unit = {
    log.info(s"read raw data from disk by $key")
    keywordsSource(path)
      .map(_.utf8String)
      .filter(_.toLowerCase(Locale.ROOT) startsWith key)
      .runWith(Sink.actorRef(self, onCompleteMessage = IndexingCompleted))
  }
}
