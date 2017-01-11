package linguistic.api

import akka.actor.ActorRef
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentRegions, CurrentShardRegionState}
import akka.http.scaladsl.model.HttpResponse
import akka.pattern._
import linguistic.ps.{WordsListSubTreeShardEntity, HomophonesSubTreeShardEntity}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ClusterApi(searchMaster: ActorRef)(implicit ex: ExecutionContext) extends BaseApi {
  implicit val timeout = akka.util.Timeout(5 seconds)

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
    }
  }
}
