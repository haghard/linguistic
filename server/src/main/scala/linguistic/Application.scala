package linguistic

import java.io.File
import java.util.TimeZone
import java.time.LocalDateTime
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
//import akka.cluster.metrics.ClusterMetricsExtension

import scala.collection._

/*

java -d64 -server
-XX:MaxGCPauseMillis=400 -XX:+UseStringDeduplication -Xmx1024m -XX:+UseG1GC -XX:ConcGCThreads=4
-XX:ParallelGCThreads=4 -Dcom.sun.management.jmxremote.port=1089 -Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.rmi.port=1089 -Dcom.sun.management.jmxremote=true
-Dakka.remote.netty.tcp.port=2551 -Dakka.http.port=9443 -Djava.rmi.server.hostname= -DENV=production
-cp /app/conf -jar /app/linguistic-0.1.jar

*/

object Application extends App with AppSupport {
  //-Duser.timezone=UTC
  //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val opts: Map[String, String] = argsToOpts(args.toList)
  applySystemProperties(opts)

  val tcpPort = System.getProperty("akka.remote.netty.tcp.port")
  val httpPort = System.getProperty("akka.http.port")
  val hostName = System.getProperty("HOSTNAME")
  val confPath = System.getProperty("CONFIG")

  val httpConf =
    s"""
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

  val httpConf1 = httpConf.replaceAll("%port%", tcpPort).replaceAll("%httpP%", httpPort)
    .replaceAll("%hostName%", hostName).replaceAll("%interface%", hostName)

  val confDir = new File(confPath) /*sys.env.getOrElse("CONFIG", ".")*/

  //for re~start
  //val env = sys.env.getOrElse("ENV", throw new Exception("ENV is expected"))

  //for alias
  val env = Option(System.getProperty("ENV")).getOrElse(throw new Exception("ENV is expected"))
  val configFile = new File(s"${confDir.getAbsolutePath}/" + env + ".conf")

  val config: Config =
    ConfigFactory.parseString(httpConf1)
    .withFallback(ConfigFactory.parseFile(configFile).resolve())
    .withFallback(ConfigFactory.load()) //for read seeds from env vars

  val coreSystem: ActorSystem = ActorSystem("linguistics", config)

  coreSystem.actorOf(Props(new HttpServer(
    httpPort.toInt, config.getString("akka.http.interface"),
    config.getString("akka.http.ssl.keypass"), config.getString("akka.http.ssl.storepass"))), "http-server")

  //ClusterMetricsExtension(coreSystem).settings
  //Auto-downing (DO NOT USE)

  val tz = TimeZone.getDefault.getID
  val greeting = new StringBuilder()
    .append('\n')
    .append("=================================================================================================")
    .append('\n')
    .append(s"★ ★ ★ Akka cluster: ${config.getInt("akka.remote.netty.tcp.port")} ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Akka seeds: ${config.getStringList("akka.cluster.seed-nodes")} ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Environment: ${env} Config: ${configFile.getAbsolutePath} TimeZone: $tz Started at ${LocalDateTime.now} ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Cassandra entry points: ${config.getString("cassandra.hosts")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Server online at https://${config.getString("akka.http.interface")}:${httpPort} ★ ★ ★ ")
    .append('\n')
    .append("=================================================================================================")

  coreSystem.log.info(greeting.toString)
}