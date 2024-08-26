package linguistic

import linguistic.dao.Accounts
import akka.actor.CoordinatedShutdown
import akka.Done
import akka.actor.CoordinatedShutdown._
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.RouteResult._
import akka.management.scaladsl.AkkaManagement
import linguistic.api.WebAssets

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Bootstrap {

  final private case object BindFailure extends Reason
}

case class Bootstrap(port: Int, hostName: String, keypass: String, storepass: String)(implicit
  system: akka.actor.typed.ActorSystem[_],
  ctx: ActorContext[_]
) extends SslSupport
    with ShardingSupport {
  val classicSystem = system.toClassic

  val terminationDeadline = classicSystem.settings.config
    .getDuration("akka.coordinated-shutdown.default-phase-timeout")
    .getSeconds
    .second

  implicit val ex = classicSystem.dispatcher

  val (wordRegion, homophonesRegion) = startSharding(classicSystem)

  // val targetActor = ctx.actorOf(PropsAdapter[StreamToActorMessage[FlowMessage]](TargetActor()))
  // ctx.system.systemActorOf(Accounts(), "accounts")
  // ctx.actorOf(PropsAdapter[Accounts.Protocol](Accounts()))

  val users: ActorRef[Accounts.Protocol] = ctx.spawn(Accounts(), "accounts")

  val routes = new WebAssets().route ~
    new api.SearchApi(wordRegion).route ~ new api.UsersApi(users).route ~ new api.ClusterMetricsApi().route

  Http()
    // .bindAndHandle(routes, hostName, port, connectionContext = https(keypass, storepass))
    .newServerAt(hostName, port)
    // .enableHttps(httpsFsa())
    .enableHttps(httpsSelfSign())
    // .enableHttps(https(keypass, storepass))
    .bind(routes)
    // .bindAndHandle(routes, hostName, port)
    .onComplete {
      case Failure(ex) =>
        classicSystem.log.error(s"Shutting down because can't bind on $hostName:$port", ex)
        CoordinatedShutdown(classicSystem).run(Bootstrap.BindFailure)
      case Success(binding) =>
        // 👍✅🚀🧪❌😄📣🔥🚨😱🥳❤️,😄,😐,😞
        classicSystem.log.info(s"★ ★ ★ Listening for HTTP connections on ${binding.localAddress} * * *")

        CoordinatedShutdown(classicSystem).addTask(PhaseBeforeServiceUnbind, "before-unbind") { () =>
          Future {
            classicSystem.log.info("★ ★ ★ CoordinatedShutdown [before-unbind] ★ ★ ★")
            Done
          }
        }

        CoordinatedShutdown(classicSystem).addTask(PhaseServiceUnbind, "http-api.unbind") { () =>
          // No new connections are accepted. Existing connections are still allowed to perform request/response cycles
          binding.unbind().map { done =>
            classicSystem.log.info("★ ★ ★ CoordinatedShutdown [http-api.unbind] ★ ★ ★")
            done
          }
        }

        CoordinatedShutdown(classicSystem).addTask(PhaseServiceUnbind, "akka-management.stop") { () =>
          AkkaManagement(classicSystem).stop().map { done =>
            classicSystem.log.info("CoordinatedShutdown [akka-management.stop]")
            done
          }
        }

        // graceful termination request being handled on this connection
        CoordinatedShutdown(classicSystem).addTask(PhaseServiceRequestsDone, "http-api.terminate") { () =>
          /** It doesn't accept new connection but it drains the existing connections Until the `terminationDeadline`
            * all the req that had been accepted will be completed and only than the shutdown will continue
            */
          binding.terminate(terminationDeadline).map { _ =>
            classicSystem.log.info("★ ★ ★ CoordinatedShutdown [http-api.terminate]  ★ ★ ★")
            Done
          }
        }

        // forcefully kills connections that are still open
        CoordinatedShutdown(classicSystem).addTask(PhaseServiceStop, "close.connections") { () =>
          Http().shutdownAllConnectionPools().map { _ =>
            classicSystem.log.info("CoordinatedShutdown [close.connections]")
            Done
          }
        }

        CoordinatedShutdown(classicSystem).addTask(PhaseActorSystemTerminate, "system.term") { () =>
          Future.successful {
            classicSystem.log.info("CoordinatedShutdown [system.term]")
            Done
          }
        }
    }
}
