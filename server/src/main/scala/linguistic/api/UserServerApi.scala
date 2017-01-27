/*

package linguistic.api

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.actor.ActorSystem
import linguistic.dao.UsersRepo
import shared.UsersApi2
import shared.protocol.SignInResponse
import upickle.Js
import upickle.default._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object AutowireServer extends autowire.Server[Js.Value, Reader, Writer] {
  def read[Result: Reader](p: Js.Value) = upickle.default.readJs[Result](p)

  def write[Result: Writer](r: Result) = upickle.default.writeJs(r)
}

object UserServerApi extends UsersApi2 {
  val Ch = StandardCharsets.UTF_8
  var system: ActorSystem = _
  var repo: UsersRepo = _
  implicit var ex: ExecutionContext = _

  def apply(repo0: UsersRepo, system0: ActorSystem) = {
    system = system0
    repo = repo0
    ex = system.dispatchers.lookup("shard-dispatcher")
    this
  }

  override def signin(headerName: String, headerValue: String): Either[String, SignInResponse] = {
    val decoded = new String(Base64.getDecoder.decode(headerValue.stripPrefix(shared.HttpSettings.salt)), Ch)
    val loginPassword = decoded.split(shared.Routes.Separator)
    if (loginPassword.length == 2) {
      system.log.info(s"sign-in request login: ${loginPassword(0)} ")
      val f = repo.signIn(loginPassword(0), loginPassword(1), system.log).map {
        case Right(response) => Right(response)
        case Left(loginError) => Left(loginError)
      }.recover { case ex: Exception => Left("Db access error: " + ex.getMessage) }
      Await.result(f, 5 second)
    } else Left("Invalid credential header for signin")
  }

  override def signup(headerName: String, headerValue: String): Either[String, String] = {
    system.log.info("Name: {}  Value: {}", headerName, headerValue)
    implicit val ex = system.dispatchers.lookup("shard-dispatcher")
    //import scala.concurrent.ExecutionContext.Implicits.global
    val decoded = new String(Base64.getDecoder.decode(headerValue.stripPrefix(shared.HttpSettings.salt)), Ch)
    val profile = decoded.split(shared.Routes.Separator)
    if (profile.length == 3) {
      val login = profile(0)
      val password = profile(1)
      val photo = profile(2)
      val f = repo.signUp(login, password, photo).map {
        case Right(true) => Right(login)
        case Right(false) => Left(s"Login ${login} isn't unique")
        case Left(error) => Left(error)
      }
      Await.result(f, 5 second)
    } else Left("Invalid credential header for signup")
  }
}*/
