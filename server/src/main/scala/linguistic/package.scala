import akka.actor.ActorDSL._
import akka.actor.{ActorRef, ActorRefFactory, ReceiveTimeout, Terminated}
import akka.cluster.sharding.ShardRegion
import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import play.twirl.api.{Html, Txt, Xml}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

package object linguistic {

  /** Twirl marshallers for Xml, Html and Txt mediatypes */
  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)
  implicit val twirlTxtMarshaller = twirlMarshaller[Txt](`text/plain`)
  implicit val twirlXmlMarshaller = twirlMarshaller[Xml](`text/xml`)

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] = {
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  }

  case class ServerSession(login: String)

  case class JoinCmd(name: String, password: String)

  implicit class FutureOpts[T](lf: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(th: Throwable) = p failure th
        def onSuccess(result: T) = p success result
      })
      p.future
    }
  }

  import akka.pattern.ask
  def gracefulShutdown(shardRegion: ActorRef)
    (implicit timeout: FiniteDuration, factory: ActorRefFactory, ec: ExecutionContext): Future[Unit] = {
    val p = Promise[Unit]()
    actor(new Act {
      context watch shardRegion
      context setReceiveTimeout timeout
      shardRegion ! ShardRegion.GracefulShutdown
      become {
        case Terminated(`shardRegion`) => p.success(())
        case ReceiveTimeout => p.failure(new Exception("Timeout to stop local shard region"))
      }
    })
    p.future
  }
}