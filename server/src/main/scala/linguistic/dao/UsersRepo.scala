package linguistic.dao

import java.net.InetSocketAddress
import java.time.Instant

import akka.actor.{Actor, ActorLogging, Props}
import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.WriteTimeoutException
import org.mindrot.jbcrypt.BCrypt
import shared.protocol.SignInResponse
import akka.pattern.pipe

import scala.collection.JavaConverters._

object UsersRepo {
  case object Activate
  case class SignIn(login: String, password: String)
  case class SignUp(login: String, password: String, photo: String)

  def props() = Props(new UsersRepo).withDispatcher("shard-dispatcher")
}

class UsersRepo extends Actor with ActorLogging {
  import UsersRepo._
  import linguistic._
  //https://datastax.github.io/java-driver/manual/custom_codecs/extras/
  import com.datastax.driver.extras.codecs.jdk8.InstantCodec

  val selectUser = "SELECT login, password, photo FROM users where login = ?"
  val insertUsers = "INSERT INTO users(login, password, photo, created_at) VALUES (?, ?, ?, ?) IF NOT EXISTS"

  val cassandraPort = context.system.settings.config.getInt("cassandra.port")
  val keySpace = context.system.settings.config.getString("cassandra.keyspace")
  val cassandraHosts = context.system.settings.config.getString("cassandra.hosts").split(",").toSeq.map(new InetSocketAddress(_, cassandraPort))
  implicit val ex = context.system.dispatchers.lookup("shard-dispatcher")

  def idle: Receive = {
    case Activate =>
      val cluster = Cluster.builder()
        .addContactPointsWithPorts(cassandraHosts.asJava)
        //.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(localDC).build())
        .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.QUORUM))
        .build

      val session = (cluster connect keySpace)

      cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance)

      //for 1 dc
      val insertStmt = (session prepare insertUsers)
      insertStmt.setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
      insertStmt.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

      val selectStmt = (session prepare selectUser)
      selectStmt.setConsistencyLevel(ConsistencyLevel.SERIAL)
      selectStmt.enableTracing

      session.execute(s"CREATE TABLE IF NOT EXISTS ${keySpace}.users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login))")

      log.info("******************* UsersRepo has been activated *************************")
      (context become active(session, selectStmt, insertStmt))
  }

  override def receive = idle

  import akka.pattern._
  def active(session: Session, selectStmt: PreparedStatement, insertStmt: PreparedStatement): Receive = {
    case SignIn(login, password) =>
      val replyTo = sender()
      session.executeAsync(selectStmt.bind(login)).asScala.map { r =>
        val queryTrace = r.getExecutionInfo.getQueryTrace
        log.info("Trace id: {}", queryTrace.getTraceId)
        val row = r.one
        if (row eq null) Left(s"Couldn't find user $login")
        else if (BCrypt.checkpw(password, row.getString("password"))) {
          Right(SignInResponse(row.getString("login"), row.getString("photo")))
        }
        else Left("Something went wrong. Password mismatches")
      }.pipeTo(replyTo)

    case SignUp(login, password, photo) =>
      val createdAt = Instant.now()
      val replyTo = sender()
      session.executeAsync(insertStmt.bind(login, BCrypt.hashpw(password, BCrypt.gensalt), photo, createdAt)).asScala
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
        }.pipeTo(replyTo)
  }
}