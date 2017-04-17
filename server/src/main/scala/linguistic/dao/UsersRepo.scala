package linguistic.dao

import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.WriteTimeoutException
import org.mindrot.jbcrypt.BCrypt
import shared.protocol.SignInResponse

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class UsersRepo()(implicit system: ActorSystem, ex: ExecutionContext) {

  import linguistic._

  val active = new AtomicBoolean()
  val cassandraPort = system.settings.config.getInt("cassandra.port")
  val keySpace = system.settings.config.getString("cassandra.keyspace")
  val cassandraHosts = system.settings.config.getString("cassandra.hosts").split(",").toSeq.map(new InetSocketAddress(_, cassandraPort))

  val cluster = Cluster.builder()
    .addContactPointsWithPorts(cassandraHosts.asJava)
    //.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(localDC).build())
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.QUORUM))
    .build

  val session = cluster.connect(keySpace)
  val log = system.log

  //https://datastax.github.io/java-driver/manual/custom_codecs/extras/
  import com.datastax.driver.extras.codecs.jdk8.InstantCodec

  cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance)

  val selectUser = "SELECT login, password, photo FROM users where login = ?"
  val insertUsers = "INSERT INTO users(login, password, photo, created_at) VALUES (?, ?, ?, ?) IF NOT EXISTS"

  private val insertStmt = session.prepare(insertUsers)

  //for 1 dc
  insertStmt.setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
  insertStmt.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

  private val selectStmt = session prepare selectUser
  selectStmt.setConsistencyLevel(ConsistencyLevel.SERIAL)
  selectStmt.enableTracing

  private def createUsersTable = {
    session.execute(s"CREATE TABLE IF NOT EXISTS ${keySpace}.users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login))")
    active.compareAndSet(false, true)
  }

  def signIn(login: String, password: String, log: LoggingAdapter)(implicit ex:ExecutionContext): Future[Either[String, SignInResponse]] = {
    if(active.get) {
      session.executeAsync(selectStmt.bind(login)).asScala.map { r =>
        val queryTrace = r.getExecutionInfo.getQueryTrace
        log.info("Trace id: {}", queryTrace.getTraceId)

        val row = r.one
        if (row eq null) Left(s"Couldn't find user $login")
        else if (BCrypt.checkpw(password, row.getString("password"))) {
          Right(SignInResponse(row.getString("login"), row.getString("photo")))
        }
        else Left("Something went wrong. Password mismatches")
      }
    } else {
      createUsersTable
      signIn(login, password, log)
    }
  }

  def signUp(login: String, password: String, photo: String)(implicit ex:ExecutionContext): Future[Either[String, Boolean]] = {
    if(active.get) {
      val createdAt = Instant.now()
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
        }
    } else {
      createUsersTable
      signUp(login, password, photo)
    }
  }
}