package linguistic

import akka.util.Timeout
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.cluster.sharding.ShardRegion
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import linguistic.protocol.{HomophonesQuery, WordsQuery}
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentShardRegionState}
import scala.concurrent.duration._

object Searches {
  def props(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) =
    Props(new Searches(mat, wordslist, homophones)).withDispatcher("shard-dispatcher")
}

class Searches(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher

  implicit val timeout = Timeout(10.seconds)

  override def receive: Receive = {
    case search: WordsQuery =>
      (wordslist ? search).pipeTo(sender())
    case search: HomophonesQuery =>
      (homophones ? search).pipeTo(sender())

    //works only for local
    case (name: String, m @ ShardRegion.GetShardRegionState) =>
      name match {
        case WordShardEntity.Name =>
          (wordslist ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name =>
          (homophones ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
      }

    case (name: String, m @ ShardRegion.GetCurrentRegions) =>
      name match {
        case WordShardEntity.Name =>
          (wordslist ? m).pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name =>
          (homophones ? m).pipeTo(sender())
        case _ => //
      }

    case (name: String, m @ ShardRegion.GetClusterShardingStats(to)) =>
      name match {
        case WordShardEntity.Name =>
          (wordslist ? m).mapTo[ClusterShardingStats].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name =>
          (homophones ? m).mapTo[ClusterShardingStats].pipeTo(sender())
        case _ => //
      }
  }
}