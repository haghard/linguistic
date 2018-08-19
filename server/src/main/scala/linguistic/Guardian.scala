package linguistic

import java.util.TimeZone
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, Props, SupervisorStrategy, Terminated}

import scala.util.{Failure, Success}

object Guardian {
  def props(env: String, httpPort: Int, hostName: String) =
    Props(new Guardian(env, httpPort, hostName)).withDispatcher(Bootstrap.HttpDispatcher)
}

class Guardian(env: String, httpPort: Int, hostName: String) extends Actor with ActorLogging {
  val system = context.system
  val config = context.system.settings.config

  val bootstrap = context.actorOf(Bootstrap.props(httpPort, hostName,
    config.getString("akka.http.ssl.keypass"),
    config.getString("akka.http.ssl.storepass")), "http-server")

  context.watch(bootstrap)

  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  println(Console.GREEN +
    """
                ___   ___   ___  __   __  ___   ___
               / __| | __| | _ \ \ \ / / | __| | _ \
               \__ \ | _|  |   /  \ V /  | _|  |   /
               |___/ |___| |_|_\   \_/   |___| |_|_\

          """ + Console.RESET)

  val tz = TimeZone.getDefault.getID
  val greeting = new StringBuilder()
    .append('\n')
    .append("=================================================================================================")
    .append('\n')
    .append(s"★ ★ ★  Environment: ${env} TimeZone: $tz Started at ${LocalDateTime.now}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★  ConstructR service-discovery: ${config.getString("constructr.coordination.class-name")} on ${config.getString("constructr.coordination.host")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★  Akka cluster: ${config.getInt("akka.remote.netty.tcp.port")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★  Akka seeds: ${config.getStringList("akka.cluster.seed-nodes")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★  Cassandra domain points: ${config.getStringList("cassandra-journal.contact-points")}  ★ ★ ★")
    .append('\n')
    .append(s"★ ★ ★  Server online at https://${config.getString("akka.http.interface")}:${httpPort}   ★ ★ ★")
    .append('\n')
    .append("=================================================================================================")
  system.log.info(greeting.toString)

  override def receive: Receive = {
    case Terminated(`bootstrap`) =>
      log.error("Terminating the system because Bootstrap terminated !!!")
      // This is NOT an Akka thread pool (since we're shutting them down)
      context.system.terminate()
        .onComplete {
          case Success(_) =>
            println("Shutdown is completed, exiting JVM with code 0")
            System.exit(0)
          case Failure(ex) =>
            println(s"Shutdown failed because of ${ex.getMessage}, exiting JVM with code -1")
            System.exit(-1)
        }(scala.concurrent.ExecutionContext.global)
  }
}