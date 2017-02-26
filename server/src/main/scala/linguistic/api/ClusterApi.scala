package linguistic.api

import akka.actor.{ActorSystem, ActorRef}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentRegions, CurrentShardRegionState}
import akka.http.scaladsl.model.HttpResponse
import akka.pattern._
import linguistic.utils.ShutdownCoordinator
import linguistic.{HttpServer, ps}
import ShutdownCoordinator.NodeShutdownOpts
import linguistic.ps.{WordShardEntity, HomophonesSubTreeShardEntity}

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

class ClusterApi(http: ActorRef, searchMaster: ActorRef, regions: Set[ActorRef])
  (implicit ex: ExecutionContext, sys: ActorSystem) extends BaseApi {
  implicit val timeout = akka.util.Timeout(10 seconds)

  val route = pathPrefix("cluster") {
    (get & path(Segment / "regions")) { seq =>
      complete {
        (searchMaster ?(seq, ShardRegion.GetCurrentRegions)).mapTo[CurrentRegions].map { r =>
          HttpResponse(entity = r.regions.mkString(","))
        }
      }
    } ~ (get & path(Segment / "shards")) { seq =>
      complete {
        (searchMaster ? (seq, ShardRegion.GetShardRegionState)).mapTo[CurrentShardRegionState].map { r =>
          HttpResponse(entity = r.shards.mkString(","))
        }
      }
    } ~ (get & path(Segment / "shards2")) { seq =>
      complete {
        (searchMaster ? (seq, ShardRegion.GetClusterShardingStats(5 seconds))).mapTo[ClusterShardingStats].map { r =>
          HttpResponse(entity = r.regions.mkString(","))
        }
      }
    }/* ~ (get & path("stop")) {
      complete {
        http ! HttpServer.Stop

        Future {
          ShutdownCoordinator.shutdown(NodeShutdownOpts(5 seconds, 20 seconds), regions)(sys)
        }(scala.concurrent.ExecutionContext.global)

        "Shutdown ..."
      }
    }*/
  }
}
