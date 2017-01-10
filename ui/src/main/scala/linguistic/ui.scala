package linguistic

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import shared.protocol.SignInResponse

import scala.concurrent.Future
import scala.util.control.NonFatal
import upickle.default._

object ui {
  case class UiSession(entry: Option[SignInResponse], token: Option[String], error: Option[String] = None)

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def httpSignUp(url: String, hs: Map[String, String]): Future[Either[String, String]] = {
    Ajax.post(url, headers = hs).map { r =>
      val token = r.getResponseHeader(shared.Headers.fromServer)
      //org.scalajs.dom.console.log("token: " + token)
      Right(token)
    }.recover { case NonFatal(e) => Left(e.getMessage) }
  }

  def httpSignIp[T: upickle.default.Reader](url: String, hs: Map[String, String]): Future[Either[String, (T, String)]] = {
    Ajax.get(url, headers = hs).map { r =>
      val token = r.getResponseHeader(shared.Headers.fromServer)
      //org.scalajs.dom.console.log("token: " + token)
      Right((read[T](r.responseText), token))
    }.recover { case NonFatal(e) => Left(e.getMessage) }
  }

  def signInHeader(name: String, password: String): (String, String) = {
    val encodedBase64 = shared.HttpSettings.salt + dom.window.btoa(s"$name&$password")
    shared.Headers.SignInHeader -> encodedBase64
  }

  def signUpHeader(name: String, password: String, photo: String): (String, String) = {
    val encodedBase64 = shared.HttpSettings.salt + dom.window.btoa(s"$name&$password&$photo")
    shared.Headers.SignUpHeader -> encodedBase64
  }
}