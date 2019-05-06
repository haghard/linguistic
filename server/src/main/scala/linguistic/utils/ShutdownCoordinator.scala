/*
package linguistic.utils

import java.time.Clock

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.sharding.ShardRegion
import linguistic.Bootstrap

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/*
  1 JVM gets the shutdown signal
  2 Coordinator tells all local ShardRegions to shut down gracefully
  3 Node leaves cluster
  4 Coordinator gives singletons a grace period to migrate
  5 Actor System & JVM Termination
 */
object ShutdownCoordinator {

  // Shutdown options
  final case class NodeShutdownOpts(
    nodeShutdownSingletonMigrationDelay: FiniteDuration = 5 seconds,
    actorSystemShutdownTimeout: FiniteDuration  = 20 seconds)

  // Messaging Protocol
  sealed trait NodeShutdownProtocol

  final case class RegisterRegions(regions: Set[ActorRef]) extends NodeShutdownProtocol
  case class StartNodeShutdown(shardRegions: Set[ActorRef]) extends NodeShutdownProtocol
  case object NodeLeftCluster extends NodeShutdownProtocol
  case object TerminateNode extends NodeShutdownProtocol

  // FSM State
  sealed trait State
  case object AwaitNodeShutdownInitiation extends State
  case object AwaitShardRegionsShutdown extends State
  case object AwaitClusterExit extends State
  case object AwaitNodeTerminationSignal extends State

  // FSM Data
  sealed trait Data
  final case class ManagedRegions(shardRegions: Set[ActorRef]) extends Data

  // Register ShardRegions for graceful shutdown
  def register(shutdownOpts: NodeShutdownOpts, http: ActorRef, shardRegions: Set[ActorRef])(implicit system: ActorSystem) = {
    val clock = Clock.systemDefaultZone
    val start = clock.instant
    sys.addShutdownHook {
      val stop = clock.instant
      val upTime = stop.getEpochSecond - start.getEpochSecond
      system.log.info(s"★ ★ ★ Stopping application at ${clock.instant} after being up for ${upTime} sec. ★ ★ ★ ")
      shutdown(shutdownOpts, http, shardRegions)
    }
  }

  private[linguistic] def shutdown(shutdownOpts: NodeShutdownOpts, http: ActorRef,
    shardRegions: Set[ActorRef])(implicit system: ActorSystem) = {
    // 1 - jvm gets the shutdown signal

    system.log.info("1 - Jvm gets the shutdown signal")
    val nodeShutdown = system.actorOf(Props(new GracefulShutdownCoordinator(shutdownOpts))
      .withDispatcher("shard-dispatcher"), "shutdown-coord")
    http ! Bootstrap.Stop
    nodeShutdown ! StartNodeShutdown(shardRegions)
    system.log.info("Awaiting node shutdown ...")
    Await.result(system.whenTerminated, shutdownOpts.actorSystemShutdownTimeout)
  }
}

import ShutdownCoordinator._
class GracefulShutdownCoordinator(shutdownOpts: NodeShutdownOpts)(implicit system: ActorSystem)
  extends FSM[State, Data] with ActorLogging {
  import ShutdownCoordinator._
  val cluster = Cluster(context.system)

  startWith(AwaitNodeShutdownInitiation, ManagedRegions(Set[ActorRef]()))

  // Wait for a ShardRegions to be registered
  when(AwaitNodeShutdownInitiation) {
    case Event(StartNodeShutdown(shardRegions), _) =>
      log.info("2 - node tells all local shard regions to shut down gracefully")
      // 2 - node tells all local shard regions to shut down gracefully
      shardRegions.foreach { shardRegion =>
        context watch shardRegion
        shardRegion ! ShardRegion.GracefulShutdown
      }
      log.info("Waiting for {} local shard region(s) to shut down ...", shardRegions.size)
      goto(AwaitShardRegionsShutdown) using ManagedRegions(shardRegions)
  }
  // 3 - node leaves cluster
  when(AwaitShardRegionsShutdown) {
    case Event(Terminated(actor), ManagedRegions(shardRegions)) =>
      log.info("Shard region terminated")
      if (shardRegions.contains(actor)) {
        val remainingRegions = shardRegions - actor
        if (remainingRegions.isEmpty) {
          log.info("All local shard region terminated.")
          log.info("3 - node leaves cluster")
          cluster.registerOnMemberRemoved(self ! NodeLeftCluster)
          cluster.leave(cluster.selfAddress)
          goto(AwaitClusterExit)
        } else {
          log.info("Waiting for {} local shard region(s) to shut down ...", remainingRegions.size)
          goto(AwaitShardRegionsShutdown) using ManagedRegions(remainingRegions)
        }
      } else stay()
  }

  // 4 - node gives singletons a grace period to migrate
  when(AwaitClusterExit) {
    case Event(NodeLeftCluster, _) =>
      import context.dispatcher
      log.info("4 - give singletons a grace period to migrate")
      system.scheduler.scheduleOnce(shutdownOpts.nodeShutdownSingletonMigrationDelay, self, TerminateNode)
      goto(AwaitNodeTerminationSignal)
  }
  // 5 - actor system is shutdown
  when(AwaitNodeTerminationSignal) {
    case Event(TerminateNode, _) =>
      log.info("5 - Terminating actor system ...")
      //this is NOT an Akka thread-pool (since those we're shutting down)
      val ec = scala.concurrent.ExecutionContext.global
      system.terminate().onComplete {
        case Success(ex) =>
          log.info("ActorSystem shutdown complete, killing jvm")
          System.exit(0)
        case Failure(ex) =>
          log.error("Shutdown failed: {}", ex)
          System.exit(-1)
      }(ec)

      stop()
  }

  whenUnhandled {
    case Event(msg, state) =>
      log.info(s"Received unhandled request $msg in state $stateName/$state")
      stay()
  }

  initialize()
}
 */
