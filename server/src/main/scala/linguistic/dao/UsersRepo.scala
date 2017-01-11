package linguistic.dao

import java.net.InetSocketAddress
import java.time.Instant

import akka.actor.ActorSystem
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy
import shared.protocol.SignInResponse
import com.datastax.driver.core._
import com.datastax.driver.core.exceptions.WriteTimeoutException
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

class UsersRepo()(implicit system: ActorSystem, ex: ExecutionContext) {
  import linguistic._

  //val localDC = system.settings.config.getString("cassandra.dc")
  val cassandraPort = system.settings.config.getInt("cassandra.port")
  val keySpace = system.settings.config.getString("cassandra.keyspace")
  val cassandraHosts = system.settings.config.getString("cassandra.hosts").split(",").toSeq.map(new InetSocketAddress(_, cassandraPort))

  val cluster = Cluster.builder()
    .addContactPointsWithPorts(cassandraHosts.asJava)
    //.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(localDC).build())
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE))
    .build

  val session = cluster.connect(keySpace)

  //https://datastax.github.io/java-driver/manual/custom_codecs/extras/
  import com.datastax.driver.extras.codecs.jdk8.InstantCodec
  cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance)

  val insertUsers = "INSERT INTO users(login, password, photo, created_at) VALUES (?, ?, ?, ?) IF NOT EXISTS"
  val selectUser = "SELECT login, password, photo FROM users where login = ?"

  session.execute(s"CREATE TABLE IF NOT EXISTS ${keySpace}.users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login))")

  private val insertStmt = session.prepare(insertUsers)

  //for 1 dc
  insertStmt.setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
  insertStmt.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)

  private val selectStmt = session prepare selectUser
  selectStmt.setConsistencyLevel(ConsistencyLevel.SERIAL)
  selectStmt.enableTracing

  def signIn(login: String, password: String): Future[Either[String, SignInResponse]] = {
    session.executeAsync(selectStmt.bind(login)).asScala.map { r =>
     val queryTrace = r.getExecutionInfo.getQueryTrace
     println("Trace id: %s\n", queryTrace.getTraceId)

      val row = r.one
      if (row eq null) Left("Couldn't find user " + login)
      else if (BCrypt.checkpw(password, row.getString("password"))) {
        Right(SignInResponse(row.getString("login"), row.getString("photo")))
      }
      else Left("Something went wrong. Password mismatches")
    }.recover { case ex: Exception => Left("Db access error: " + ex.getMessage) }
  }

  def signUp(login: String, password: String, photo: String): Future[Either[String, Boolean]] = {
    val createdAt = Instant.now()
    session.executeAsync(insertStmt.bind(login, BCrypt.hashpw(password, BCrypt.gensalt), photo, createdAt)).asScala
      .map { r =>
        //log.info("inserting a new user {}", login)
        Right(r.wasApplied)
      }.recover {
      case e: WriteTimeoutException =>
        if (e.getWriteType eq WriteType.CAS) {
          //UnknownException
          Left(s"Unknown result: ${e.getMessage}")
        } else if (e.getWriteType eq WriteType.SIMPLE) {
          //commit stage has failed
          Left(s"Commit stage has failed: ${e.getMessage}")
        } else Left(s"Unexpected write type: ${e.getMessage}")
      case ex: Exception => Left(s"Db access error: ${ex.getMessage}")
    }
  }
}