package linguistic.api

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import linguistic.dao.UsersRepo
import linguistic.{ServerSession, AuthTokenSupport}
import shared.protocol.SignInResponse
import upickle.default._

//val photo = "https://avatars.githubusercontent.com/u/1887034?v=3"
class UsersApi(users: UsersRepo)(implicit val system: ActorSystem) extends BaseApi with AuthTokenSupport {
  val Ch = StandardCharsets.UTF_8

  private def respondWithUserError(error: String) = {
    complete(HttpResponse(InternalServerError, entity = s"""{ "error": "${error}" }"""))
  }

  val route = extractMaterializer { implicit mat =>
    extractExecutionContext { implicit ec =>
      extractLog { log =>
        pathPrefix(apiPrefix) {
          post {
            path(shared.Routes.signUp) {
              headerValueByName(shared.Headers.SignUpHeader) { credentials =>
                extractHost { host =>
                  val decoded = new String(Base64.getDecoder.decode(credentials.stripPrefix(shared.HttpSettings.salt)), Ch)
                  val profile = decoded.split(shared.Routes.Separator)
                  if (profile.length == 3) {
                    val login = profile(0)
                    val pas = profile(1)
                    val photo = profile(2)
                    log.info(s"signup request from: $host login: $login")
                    onSuccess(users.signUp(login, pas, photo)) {
                      case Right(true) =>
                        setSession(oneOff, usingHeaders, ServerSession(login)) {
                          complete(HttpResponse(StatusCodes.OK))
                        }
                      case Right(false) => respondWithUserError(s"Login ${login} isn't unique")
                      case Left(errorMsg) => respondWithUserError(errorMsg)
                    }
                  } else respondWithUserError("Expected login, password and photo")
                }
              }
            }
          } ~ get {
            path(shared.Routes.signIn) {
              headerValueByName(shared.Headers.SignInHeader) { credentials =>
                extractHost { host =>
                  val decoded = new String(Base64.getDecoder.decode(credentials.stripPrefix(shared.HttpSettings.salt)), Ch)
                  val loginPassword = decoded.split(shared.Routes.Separator)
                  if (loginPassword.length == 2) {
                    log.info(s"sign-in request from: $host login: ${loginPassword(0)} ")
                    onSuccess(users.signIn(loginPassword(0), loginPassword(1))) {
                      case Right(response) =>
                        setSession(oneOff, usingHeaders, ServerSession(response.login)) {
                          complete(write[SignInResponse](response))
                        }
                      case Left(loginError) => respondWithUserError(loginError)
                    }
                  } else respondWithUserError(s"${shared.Routes.signIn} request has wrong headed")
                }
              }
            }
          }
        }
      }
    }
  }
}