package linguistic

import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.{SessionConfig, SessionManager, SessionSerializer, SingleValueSessionSerializer}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait AuthTokenSupport {
  implicit def system: akka.actor.typed.ActorSystem[_]

  private val sessionConfig   = SessionConfig.fromConfig(system.settings.config)
  implicit val sessionManager = new SessionManager[ServerSession](sessionConfig)

  implicit def serializer: SessionSerializer[ServerSession, String] =
    new SingleValueSessionSerializer({ _.login }, { line: String =>
      Success(ServerSession(line))
    })

  //oneOff vs refreshable; specifies what should happen when the session expires.
  //If refreshable and a refresh token is present, the session will be re-created
  def requiredHttpSession(implicit ec: ExecutionContext) =
    requiredSession(oneOff, usingHeaders)
}
