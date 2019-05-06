package linguistic

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import shared.protocol.SignInResponse
import scala.concurrent.Future
import upickle.default._

object gateaway {

  trait LoginMode
  case object SignInMode extends LoginMode
  case object SignUpMode extends LoginMode

  case class UiSession(
    user: Option[SignInResponse] = None,
    token: Option[String] = None,
    mode: LoginMode = SignInMode,
    error: Option[String] = None
  )

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def signUp(url: String, hs: Map[String, String]): Future[dom.XMLHttpRequest] =
    Ajax.post(url, headers = hs)

  def signIn[T: upickle.default.Reader](url: String, hs: Map[String, String]): Future[(T, String)] =
    Ajax.get(url, headers = hs).map { r =>
      val token = r.getResponseHeader(shared.Headers.fromServer)
      (read[T](r.responseText), token)
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
