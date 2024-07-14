package linguistic.api

import akka.actor.{ActorSystem, ActorRef}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentRegions, CurrentShardRegionState}
import akka.http.scaladsl.model.HttpResponse
import akka.pattern._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ClusterApi(searchMaster: ActorRef, regions: Set[ActorRef])(
  implicit ex: ExecutionContext,
  sys: ActorSystem
) extends BaseApi {
  implicit val timeout = akka.util.Timeout(5.seconds)

  val route = pathPrefix("cluster") {
    (get & path(Segment / "regions")) { shardName =>
      complete {
        (searchMaster ? (shardName, ShardRegion.GetCurrentRegions))
          .mapTo[CurrentRegions]
          .map(r => HttpResponse(entity = r.regions.mkString(",")))
      }
    } ~ (get & path(Segment / "shards")) { shardName =>
      complete {
        (searchMaster ? (shardName, ShardRegion.GetShardRegionState))
          .mapTo[CurrentShardRegionState]
          .map(r => HttpResponse(entity = r.shards.mkString(",")))
      }
    } ~ (get & path(Segment / "shards2")) { shardName =>
      complete {
        (searchMaster ? (shardName, ShardRegion.GetClusterShardingStats(timeout.duration)))
          .mapTo[ClusterShardingStats]
          .map(r => HttpResponse(entity = r.regions.mkString(",")))
      }
    }
  }
}
