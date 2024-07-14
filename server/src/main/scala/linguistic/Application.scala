package linguistic

import java.io.File
import java.time.LocalDateTime
import java.util.TimeZone
import akka.cluster.Cluster
import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import linguistic.dao.CassandraSessionExtension
import linguistic.protocol.SearchQuery

import scala.collection._

object Application extends App with AppSupport {

  val opts: Map[String, String] = argsToOpts(args.toList)
  applySystemProperties(opts)

  val tcpPort  = System.getProperty("akka.remote.artery.canonical.port")
  val httpPort = System.getProperty("akka.http.port")
  val hostName = System.getProperty("HOSTNAME")
  val confPath = System.getProperty("CONFIG")
  val dbHosts  = System.getProperty("cassandra.hosts")
  val username = System.getProperty("cassandra.username")
  val password = System.getProperty("cassandra.password")

  val env = Option(System.getProperty("ENV")).getOrElse(throw new Exception("ENV is expected"))

  val httpConf =
    s"""
       |akka.remote.artery.canonical.port=%port%
       |akka.http.port=%httpP%
       |akka.remote.artery.canonical.hostname=%hostName%
       |akka.http.interface=%interface%
       |
       |akka.http.session {
       |  header {
       |    send-to-client-name = ${shared.Headers.fromServer}
       |    get-from-client-name = ${shared.Headers.fromClient}
       |  }
       | }
       |
    """.stripMargin

  val contactPoints = dbHosts.split(",").map(h => s""" "$h" """).mkString(",").dropRight(1)

  val dbConf =
    s"""
       |cassandra-journal {
       |  contact-points = [ $contactPoints ]
       |  authentication.username = $username
       |  authentication.password = $password
       |}
       |
       |cassandra-snapshot-store {
       |  contact-points = [ $contactPoints ]
       |  authentication.username = $username
       |  authentication.password = $password
       |}
       |
    """.stripMargin

  val effectedHttpConf = httpConf
    .replaceAll("%port%", tcpPort)
    .replaceAll("%httpP%", httpPort)
    .replaceAll("%hostName%", hostName)
    .replaceAll("%interface%", hostName)

  val configFile = new File(s"${new File(confPath).getAbsolutePath}/" + env + ".conf")

  val config: Config =
    ConfigFactory
      .parseString(effectedHttpConf)
      //.withFallback(ConfigFactory.parseString(dbConf))
      .withFallback(ConfigFactory.parseFile(configFile).resolve())
      .withFallback(ConfigFactory.load()) //for read seeds from env vars


  implicit val system = ActorSystem("linguistics", config)
  //actor system level Materializer
  //implicit val mat = SystemMaterializer(system).materializer

  //CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
  CassandraSessionExtension(system).session

  Cluster(system).registerOnMemberUp {
    val greeting = new StringBuilder()
      .append('\n')
      .append("=================================================================================================")
      .append('\n')
      .append(
        s"★ ★ ★  Environment: ${env} TimeZone: ${TimeZone.getDefault.getID} Started at ${LocalDateTime.now}  ★ ★ ★"
      )
      .append('\n')
      .append(s"★ ★ ★  Akka cluster: ${config.getInt("akka.remote.artery.canonical.port")}  ★ ★ ★")
      .append('\n')
      .append(s"★ ★ ★  Akka seeds: ${config.getStringList("akka.cluster.seed-nodes")}  ★ ★ ★")
      //.append('\n')
      .append(s"★ ★ ★  Cassandra domain points: ${config.getStringList("datastax-java-driver.basic.contact-points")}  ★ ★ ★")
      //.append(s"★ ★ ★  Cassandra domain points: ${config.getStringList("cassandra-journal.contact-points")}  ★ ★ ★")
      .append('\n')
      .append(s"★ ★ ★  Server online at http://${config.getString("akka.http.interface")}:$httpPort   ★ ★ ★")
      .append('\n')
      .append("=================================================================================================")
    system.log.info(greeting.toString)

    Bootstrap(httpPort.toInt, hostName, config.getString("akka.http.ssl.keypass"), config.getString("akka.http.ssl.storepass"))
  }
}
