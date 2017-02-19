package linguistic

import java.time.LocalDateTime
import java.util.TimeZone

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, Props, SupervisorStrategy}

object DiscoveryGuardian {
  def props(env: String, httpPort: Int, hostName: String) =
    Props(new DiscoveryGuardian(env, httpPort, hostName)).withDispatcher(HttpServer.HttpDispatcher)
}

class DiscoveryGuardian(env: String, httpPort: Int, hostName: String) extends Actor with ActorLogging {
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  val system = context.system
  val config = context.system.settings.config
  val timeout = FiniteDuration(config.getDuration("constructr.join-timeout").toNanos, NANOSECONDS)
  system.scheduler.scheduleOnce(timeout)(self ! 'Discovered)(system.dispatcher)

  //system.actorSelection("/")

  override def receive: Receive = {
    case 'Discovered =>
      context.system.actorOf(
        HttpServer.props(httpPort, hostName,
          config.getString("akka.http.ssl.keypass"),
          config.getString("akka.http.ssl.storepass")
        ), "http-server")

      val tz = TimeZone.getDefault.getID
      val greeting = new StringBuilder()
        .append('\n')
        .append("=================================================================================================")
        .append('\n')
        .append(s"★ ★ ★   Environment: ${env} TimeZone: $tz Started at ${LocalDateTime.now}   ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   ConstructR service-discovery: ${config.getString("constructr.coordination.class-name")}   ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   ConstructR host: ${config.getString("constructr.coordination.host")}  ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   Akka cluster: ${config.getInt("akka.remote.netty.tcp.port")}   ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   Akka seeds: ${config.getStringList("akka.cluster.seed-nodes")}   ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   Cassandra entry points: ${config.getString("cassandra.hosts")}   ★ ★ ★")
        .append('\n')
        .append(s"★ ★ ★   Server online at https://${config.getString("akka.http.interface")}:${httpPort}   ★ ★ ★")
        .append('\n')
        .append("=================================================================================================")
      system.log.info(greeting.toString)
  }
}