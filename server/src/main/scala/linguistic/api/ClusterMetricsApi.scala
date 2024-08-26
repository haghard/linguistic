package linguistic.api

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.metrics.ClusterMetricsEvent
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.KillSwitches
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

final class ClusterMetricsApi()(implicit sys: ActorSystem[_]) extends BaseApi {

  val bs = 1 << 4
  val ((queue, ks), src) =
    Source
      .queue[ByteString](bs)
      .viaMat(KillSwitches.single)(Keep.both)
      .toMat(BroadcastHub.sink[ByteString](1))(Keep.both)
      .run()

  // to drain metrics if no active clients
  src.runWith(Sink.ignore)

  val metrics: ActorRef[ClusterMetricsEvent] =
    sys.systemActorOf(ClusteredJvmMetrics(queue), "jvm")

  val route =
    path("jvm") {
      get {
        complete(
          HttpResponse(entity =
            HttpEntity.Chunked
              .fromData(ContentTypes.`text/plain(UTF-8)`, ClusteredJvmMetrics.metricsStream(src, System.nanoTime()))
          )
        )
      }
    }

  CoordinatedShutdown(sys).addTask(PhaseBeforeServiceUnbind, "http-api.stop-clustered-metrics") { () =>
    Future.successful {
      ks.shutdown()
      sys.log.info("★ ★ ★ CoordinatedShutdown [http-api.stop-clustered-metrics] ★ ★ ★")
      Done
    }
  }

  /*val route = pathPrefix("cluster") {
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
    } ~ jvm
  }*/

}
