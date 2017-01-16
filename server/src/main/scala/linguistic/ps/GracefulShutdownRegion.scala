package linguistic.ps

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ShardRegion}

object GracefulShutdownRegion {

  case object LeaveAndShutdownNode

  def props(http: ActorRef, names: Seq[String]) =
    Props(new GracefulShutdownRegion(http, names)).withDispatcher("shard-dispatcher")
}

class GracefulShutdownRegion(http: ActorRef, names: Seq[String]) extends Actor with ActorLogging {
  import GracefulShutdownRegion._
  import scala.concurrent.duration._

  val cluster = Cluster(context.system)
  val regions = names.map(ClusterSharding(context.system).shardRegion(_))

  override def receive = {
    case LeaveAndShutdownNode =>
      regions.foreach(context watch _)
      log.info("★ ★ ★ An attempt to stop local shard {} on {} ★ ★ ★ ", names, cluster.selfAddress)
      regions.foreach(_ ! ShardRegion.GracefulShutdown)

    case Terminated(region) =>
      log.info("★ ★ ★ Terminated {} ★ ★ ★ ", region)
      cluster.registerOnMemberRemoved(self ! "member-removed")
      (cluster leave cluster.selfAddress)

    case "member-removed" ⇒
      // Let singletons hand over gracefully before stopping the system
      import context.dispatcher
      context.system.scheduler.scheduleOnce(10.seconds, self, 'stopSystem)

    case 'stopSystem ⇒
      log.info("★ ★ ★ Local shard {} on {} has been stopped successfully ★ ★ ★ ", names, cluster.selfAddress)
      http ! linguistic.HttpServer.Stop
      context.stop(self)
  }
}