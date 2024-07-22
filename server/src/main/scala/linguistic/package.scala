import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes._
import com.datastax.oss.driver.shaded.guava.common.util.concurrent.{FutureCallback, Futures, ListenableFuture, MoreExecutors}
import play.twirl.api.{Html, Txt, Xml}

import scala.concurrent.{Future, Promise}

package object linguistic {

  /** Twirl marshallers for Xml, Html and Txt mediatypes */
  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)
  implicit val twirlTxtMarshaller  = twirlMarshaller[Txt](`text/plain`)
  implicit val twirlXmlMarshaller  = twirlMarshaller[Xml](`text/xml`)

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)

  case class ServerSession(login: String)

  case class JoinCmd(name: String, password: String)

  implicit class FutureOpts[T](lf: ListenableFuture[T]) {
    def asScala: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(
        lf,
        new FutureCallback[T] {
          def onFailure(th: Throwable) = p failure th
          def onSuccess(result: T)     = p success result
        },
        MoreExecutors.directExecutor()
      )
      p.future
    }
  }
}
