package linguistic

import java.io.File
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection._

//-Duser.timezone=UTC
//TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
object Application extends App with AppSupport {
  val opts: Map[String, String] = argsToOpts(args.toList)
  applySystemProperties(opts)

  val tcpPort = System.getProperty("akka.remote.netty.tcp.port")
  val httpPort = System.getProperty("akka.http.port")
  val hostName = System.getProperty("HOSTNAME")
  val confPath = System.getProperty("CONFIG")
  val discHost = System.getProperty("DISCOVERY")
  val dbHosts = System.getProperty("cassandra.hosts")

  val httpConf =
    s"""
       |
       |akka.remote.netty.tcp.port=%port%
       |akka.http.port=%httpP%
       |akka.remote.netty.tcp.hostname=%hostName%
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

  val constructrConf =
    s"""
      |akka.extensions = [de.heikoseeberger.constructr.ConstructrExtension]
      |constructr {
      |  coordination {
      |    host = ${discHost}
      |    port = 2379
      |    class-name = de.heikoseeberger.constructr.coordination.etcd.EtcdCoordination
      |  }
      |  max-nr-of-seed-nodes = 5
      |  coordination-timeout = 5 seconds
      |  nr-of-retries        = 3
      |  join-timeout = 15 seconds
      |}
    """.stripMargin


  val contactPoints = dbHosts.split(",").map(h=> s""" "$h" """).mkString(",").dropRight(1)
  val dbConf =
    s"""
      |cassandra-journal {
      |   contact-points = [ $contactPoints ]
      |}
      |
      |cassandra-snapshot-store {
      |   contact-points = [ $contactPoints ]
      |}
      |
    """.stripMargin

  val effectedHttpConf = httpConf.replaceAll("%port%", tcpPort).replaceAll("%httpP%", httpPort)
    .replaceAll("%hostName%", hostName).replaceAll("%interface%", hostName)

  val confDir = new File(confPath) /*sys.env.getOrElse("CONFIG", ".")*/

  //for re~start
  //val env = sys.env.getOrElse("ENV", throw new Exception("ENV is expected"))

  //for alias
  val env = Option(System.getProperty("ENV")).getOrElse(throw new Exception("ENV is expected"))
  val configFile = new File(s"${confDir.getAbsolutePath}/" + env + ".conf")

  val config: Config =
    ConfigFactory.parseString(effectedHttpConf)
      .withFallback(ConfigFactory.parseString(constructrConf))
      .withFallback(ConfigFactory.parseString(dbConf))
      .withFallback(ConfigFactory.parseFile(configFile).resolve())
      .withFallback(ConfigFactory.load()) //for read seeds from env vars

  val coreSystem: ActorSystem = ActorSystem("linguistics", config)
  coreSystem.actorOf(DiscoveryGuardian.props(env, httpPort.toInt, hostName), "guardian")
  //Auto-downing (DO NOT USE)
}