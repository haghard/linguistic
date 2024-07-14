package linguistic.ps

import java.io.{File, FileInputStream}
import java.util.Locale
import akka.actor.ActorLogging
import akka.actor.typed.ActorRef
import akka.persistence.PersistentActor
import akka.stream.IOResult
import akka.stream.scaladsl.StreamConverters.fromInputStream
import akka.stream.scaladsl.{Framing, Source}
import akka.stream.typed.scaladsl.ActorSink
import akka.util.ByteString
import com.rklaehn.radixtree.RadixTree

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Indexing {

  sealed trait Confirm
  case object Confirm extends Confirm

  sealed trait IndexingProtocol
  object IndexingProtocol {
    final case class Next(replyTo: ActorRef[Confirm], terms: Seq[String]) extends IndexingProtocol
    final case class Init(replyTo: ActorRef[Confirm])                     extends IndexingProtocol
    final case object OnCompleted                                         extends IndexingProtocol
    final case class Failure(ex: Throwable)                               extends IndexingProtocol
  }

}

trait Indexing[T] {
  mixin: PersistentActor with ActorLogging =>

  implicit def sys = context.system

  type SubTree = RadixTree[String, T]

  def key: String

  def keywordsSource(path: String): Source[ByteString, Future[IOResult]] =
    fromInputStream(() => new FileInputStream(new File(path)))
      .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))

  def buildIndex(sink: ActorRef[Indexing.IndexingProtocol], key: String, path: String): Unit = {

    val actorSink =
      ActorSink.actorRefWithBackpressure[Seq[String], Indexing.IndexingProtocol, Indexing.Confirm](
        ref = sink,
        messageAdapter = Indexing.IndexingProtocol.Next(_, _),
        onInitMessage = Indexing.IndexingProtocol.Init,
        ackMessage = Indexing.Confirm,
        onCompleteMessage = Indexing.IndexingProtocol.OnCompleted,
        onFailureMessage = Indexing.IndexingProtocol.Failure(_)
      )

    log.info(s"--- buildIndex($key): Read raw data from disk by $key")
    keywordsSource(path)
      .map(_.utf8String)
      .filter { strLine =>
        val w = strLine.trim
        w.nonEmpty && w.toLowerCase(Locale.ROOT).startsWith(key) && java.lang.Character.isLetter(w.head)
      }.map(_.takeWhile(!_.isDigit).trim)
      .groupedWithin(1 << 10, 350.millis)
      .throttle(1, 300.millis)
      .to(actorSink)
      .run()
  }
}
