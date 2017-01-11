package linguistic

import java.io.File
import java.time.{Clock, LocalDateTime}
import java.util.TimeZone

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.{Config, ConfigFactory}
import linguistic.dao.UsersRepo
import scala.collection._
import scala.util.Try

object Application extends App with AppSupport with SslSupport {
  //-Duser.timezone=UTC
  //TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  val HttpDispatcher = "akka.http.dispatcher"

  //
  val opts: Map[String, String] = argsToOpts(args.toList)
  applySystemProperties(opts)

  val port = System.getProperty("akka.remote.netty.tcp.port")
  val httpPort = System.getProperty("akka.http.port")
  val hostName = System.getProperty("HOSTNAME")


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

  val httpConf1 = httpConf.replaceAll("%port%", port).replaceAll("%httpP%", httpPort)
    .replaceAll("%hostName%", hostName).replaceAll("%interface%", hostName)

  val confDir = new File(sys.env.getOrElse("CONFIG", "."))

  //for re~start
  //val env = sys.env.getOrElse("ENV", throw new Exception("ENV is expected"))

  //for alias
  val env = Option(System.getProperty("ENV")).getOrElse(throw new Exception("ENV is expected"))
  val configFile = new File(s"${confDir.getAbsolutePath}/" + env + ".conf")

  val config: Config = ConfigFactory.parseString(httpConf1)
    .withFallback(ConfigFactory.parseFile(configFile).resolve())

  implicit val system = ActorSystem("linguistics", config)
  implicit val ex = system.dispatchers.lookup("shard-dispatcher")

  implicit val mat = akka.stream.ActorMaterializer(
      ActorMaterializerSettings.create(system = system)
        .withDispatcher(HttpDispatcher))

  val httpP = config.getInt("akka.http.port")
  val interface = config.getString("akka.http.interface")
  val host0 = config.getString("akka.remote.netty.tcp.hostname")
  println(host0)

  val sm = system.actorOf(SearchMaster.props(mat), "search-master")
  val users = new UsersRepo()

  val routes = new api.SearchApi(sm).route ~ new api.Nvd3Api().route ~
    new api.UsersApi(users).route ~ new api.ClusterApi(sm).route

  Http().bindAndHandle(routes, interface, httpP, connectionContext =
    https(config.getString("akka.http.ssl.keypass"), config.getString("akka.http.ssl.storepass")))


  val clock = Clock.systemDefaultZone
  val start = clock.instant
  sys.addShutdownHook {
    val stop = clock.instant
    val upTime = stop.getEpochSecond - start.getEpochSecond
    system.log.info(s"★ ★ ★  Stopping application at ${clock.instant} after being up for ${upTime} sec. ★ ★ ★ ")
  }

  val tz = TimeZone.getDefault.getID
  val greeting = new StringBuilder()
    .append('\n')
    .append("=================================================================================================")
    .append('\n')
    .append(s"★ ★ ★ Akka cluster: ${config.getInt("akka.remote.netty.tcp.port")} ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Environment: ${env} Config: ${configFile.getAbsolutePath} TimeZone: $tz Started at ${LocalDateTime.now} ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Cassandra entry points: ${config.getString("cassandra.hosts")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★ Server online at https://$interface:$httpP  ★ ★ ★ ")
    .append('\n')
    .append("=================================================================================================")

  system.log.info(greeting.toString)
}