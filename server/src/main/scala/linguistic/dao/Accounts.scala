package linguistic.dao

import java.time.Instant
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import org.mindrot.jbcrypt.BCrypt
import shared.protocol.SignInResponse

import scala.util.{Failure, Success, Try}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import com.datastax.oss.driver.api.core.cql.{PreparedStatement, SimpleStatement}
import com.datastax.oss.driver.api.core.servererrors.{WriteTimeoutException, WriteType}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.util.control.NonFatal

object Accounts {
  case object Activate
  case class SignIn(login: String, password: String)
  case class SignUp(login: String, password: String, photo: String)

  def retry(f: () => CassandraSession, n: Int): CassandraSession =
    Try(f()) match {
      case Success(r) => r
      case Failure(ex) =>
        if (n > 0) {
          Thread.sleep(3000)
          retry(f, n - 1)
        } else throw ex
    }

  val selectUser  = "SELECT login, password, photo FROM users where login = ?"
  val insertUsers = "INSERT INTO users(login, password, photo, created_at) VALUES (?, ?, ?, ?) IF NOT EXISTS"

  def props = Props(new Accounts).withDispatcher("shard-dispatcher")
}

class Accounts extends Actor with ActorLogging {
  import Accounts._
  implicit val ex = context.system.dispatchers.lookup("shard-dispatcher")

  //https://datastax.github.io/java-driver/manual/custom_codecs/extras/
  //import com.datastax.driver.extras.codecs.jdk8.InstantCodec
  
  val session =
    CassandraSessionExtension(context.system).session


  def idle: Receive = {
    case Activate =>

      val insert = SimpleStatement
        .builder(insertUsers)
        //.setExecutionProfileName(profileName)
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
      //selectStmt.setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
      //selectStmt.setConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
      //selectStmt.enableTracing
      context become active(session, selectStmt, insertStmt)
  }

  override def receive: Receive = idle

  def active(session: CqlSession, selectStmt: PreparedStatement, insertStmt: PreparedStatement): Receive = {
    case reply: Either[String, _] @unchecked =>
       context.system.log.info(s"Got $reply")

    case SignIn(login, password) =>
      val replyTo = sender()
      new CompletionStageOps(session.executeAsync(selectStmt.bind(login))).toScala
        .map { res =>
          val row = res.one()
           if(row ne null) {
              if (BCrypt.checkpw(password, row.getString("password")))
                Right(SignInResponse(row.getString("login"), row.getString("photo")))
              else
                Left("Something went wrong. Password mismatches")

           } else Left(s"Couldn't find user $login")
        }.recover {
          case NonFatal(ex) =>
            Left(ex.getMessage)
        }
        .pipeTo(replyTo)

    case SignUp(login, password, photo) =>
      val createdAt = Instant.now()
      val replyTo   = sender()
      new CompletionStageOps(
        session.executeAsync(insertStmt.bind(login, BCrypt.hashpw(password, BCrypt.gensalt), photo, createdAt))
      ).toScala
        .map(r => Right(r.wasApplied))
        .recover {
          case e: WriteTimeoutException =>
            log.error(e, "Cassandra write error :")
            if (e.getWriteType eq WriteType.CAS) {
              //UnknownException
              Left(s"Unknown result: ${e.getMessage}")
            } else if (e.getWriteType eq WriteType.SIMPLE) {
              //commit stage has failed
              Left(s"Commit stage has failed: ${e.getMessage}")
            } else {
              Left(s"Unexpected write type: ${e.getMessage}")
            }
          case ex: Exception =>
            log.error(ex, "Cassandra error :")
            Left(s"Db access error: ${ex.getMessage}")
        }
        .pipeTo(replyTo)
  }
}
