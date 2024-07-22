package linguistic.dao

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import com.datastax.oss.driver.api.core.cql.{PreparedStatement, SimpleStatement}
import com.datastax.oss.driver.api.core.servererrors.{WriteTimeoutException, WriteType}
import org.mindrot.jbcrypt.BCrypt
import shared.protocol.SignInResponse

import java.time.Instant
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.util.control.NonFatal

object Accounts {

  sealed trait Protocol
  object Protocol {
    final case class SignIn(login: String, password: String, replyTo: ActorRef[String Either SignInResponse])         extends Protocol
    final case class SignUp(login: String, password: String, photo: String, replyTo: ActorRef[String Either Boolean]) extends Protocol
  }

  def apply(): Behavior[Protocol] =
    Behaviors.setup { implicit ctx =>
      val session =
        CassandraSessionExtension(ctx.system).session

      val selectUser  = "SELECT login, password, photo FROM users where login = ?"
      val insertUsers = "INSERT INTO users(login, password, photo, created_at) VALUES (?, ?, ?, ?) IF NOT EXISTS"

      val insert = SimpleStatement
        .builder(insertUsers)
        .setConsistencyLevel(ConsistencyLevel.ONE)
        .build()

      //for 1 dc
      val insertStmt = session.prepare(insert)
      //insertStmt.setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
      //insertStmt.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

      val select = SimpleStatement
        .builder(selectUser)
        //.setExecutionProfileName(profileName)
        .setConsistencyLevel(ConsistencyLevel.ONE)
        .build()

      val selectStmt = session.prepare(select)

      active(session, selectStmt, insertStmt)
    }

  def active(
    session: CqlSession,
    selectStmt: PreparedStatement,
    insertStmt: PreparedStatement
  )(implicit ctx: ActorContext[Protocol]): Behavior[Protocol] =
    Behaviors.receiveMessagePartial {
      case Protocol.SignIn(login, password, replyTo) =>
        import ctx.executionContext
        new CompletionStageOps(session.executeAsync(selectStmt.bind(login))).toScala
          .map { res =>
            val row = res.one()
            if (row ne null)
              if (BCrypt.checkpw(password, row.getString("password")))
                replyTo.tell(Right(SignInResponse(row.getString("login"), row.getString("photo"))))
              else
                replyTo.tell(Left("Something went wrong. Password mismatches"))
            else
              replyTo.tell(Left(s"Couldn't find user $login"))
          }
          .recover { case NonFatal(ex) =>
            replyTo.tell(Left(ex.getMessage))
          }
        Behaviors.same

      case Protocol.SignUp(login, password, photo, replyTo) =>
        import ctx.executionContext
        val createdAt = Instant.now()
        val logger    = ctx.log
        new CompletionStageOps(
          session.executeAsync(insertStmt.bind(login, BCrypt.hashpw(password, BCrypt.gensalt), photo, createdAt))
        ).toScala
          .map(r => replyTo.tell(Right(r.wasApplied)))
          .recover {
            case e: WriteTimeoutException =>
              logger.error("Cassandra write error.", e)
              if (e.getWriteType eq WriteType.CAS)
                //UnknownException
                Left(s"Unknown result: ${e.getMessage}")
              else if (e.getWriteType eq WriteType.SIMPLE)
                //commit stage has failed
                Left(s"Commit stage has failed: ${e.getMessage}")
              else
                Left(s"Unexpected write type: ${e.getMessage}")
            case ex: Exception =>
              logger.error("Cassandra error.", ex)
              Left(s"Db access error: ${ex.getMessage}")
          }

        Behaviors.same
    }
}
