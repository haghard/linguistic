package linguistic

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import linguistic.dao.Guardian

import scala.collection._
import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

object Application extends App with AppSupport {

  val opts: Map[String, String] = argsToOpts(args.toList)
  applySystemProperties(opts)

  val tcpPort  = System.getProperty("akka.remote.artery.canonical.port")
  val httpPort = System.getProperty("akka.http.port")
  val hostName = System.getProperty("akka.remote.artery.canonical.hostname")
  val confPath = System.getProperty("CONFIG")
  val dbHosts  = System.getProperty("cassandra.hosts")
  val username = System.getProperty("cassandra.username")
  val password = System.getProperty("cassandra.password")
  val env      = Option(System.getProperty("ENV")).getOrElse(throw new Exception("ENV is expected"))

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
      // .withFallback(ConfigFactory.parseString(dbConf))
      .withFallback(ConfigFactory.parseFile(configFile).resolve())
      .withFallback(ConfigFactory.load()) // for read seeds from env vars

  val keypass   = config.getString("akka.http.ssl.keypass")
  val storepass = config.getString("akka.http.ssl.storepass")

  implicit val system = akka.actor.typed.ActorSystem(
    Guardian(httpPort.toInt, hostName, keypass, storepass),
    "linguistics",
    config
  )

  akka.management.scaladsl.AkkaManagement(system).start()
  akka.management.cluster.bootstrap.ClusterBootstrap(system).start()
  // akka.discovery.Discovery(system).loadServiceDiscovery("config") // kubernetes-api

  val _ = scala.io.StdIn.readLine()
  system.terminate()
  system.log.warn("★ ★ ★ ★ ★ ★  Shutting down ... ★ ★ ★ ★ ★ ★")
  Await.result(
    system.whenTerminated,
    FiniteDuration(
      system.settings.config.getDuration("akka.coordinated-shutdown.default-phase-timeout").toNanos,
      NANOSECONDS
    )
  )
  sys.exit(0)
}
