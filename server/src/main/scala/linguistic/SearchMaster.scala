package linguistic

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import linguistic.WordsSearchProtocol.{SearchHomophones, SearchWord}
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentShardRegionState}

import scala.concurrent.duration._

object SearchMaster {
  def props(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) =
    Props(new SearchMaster(mat, wordslist, homophones)).withDispatcher("shard-dispatcher")
}

class SearchMaster(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(10.seconds)

  override def receive: Receive = {
    case search: SearchWord => (wordslist ? search).pipeTo(sender())
    case search: SearchHomophones => (homophones ? search).pipeTo(sender())

    //works only for local
    case (name: String, m @ ShardRegion.GetShardRegionState) =>
      name match {
        case WordShardEntity.Name => (wordslist ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
      }

    case (name: String, m @ ShardRegion.GetCurrentRegions) =>
      name match {
        case WordShardEntity.Name => (wordslist ? m).pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).pipeTo(sender())
      }

    case (name: String, m @ ShardRegion.GetClusterShardingStats(to)) =>
      name match {
        case WordShardEntity.Name => (wordslist ? m).mapTo[ClusterShardingStats].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).mapTo[ClusterShardingStats].pipeTo(sender())
      }
  }
}