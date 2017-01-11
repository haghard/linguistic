package linguistic

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.sharding.ShardCoordinator.LeastShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentShardRegionState}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.{ask, pipe}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import linguistic.WordsSearchProtocol.{SearchHomophones, SearchWord}
import linguistic.ps.{HomophonesSubTreeShardEntity, WordsListSubTreeShardEntity}

import scala.concurrent.duration._

object SearchMaster {
  def props(mat: ActorMaterializer) =
    Props(new SearchMaster(mat)).withDispatcher("shard-dispatcher")
}

class SearchMaster(mat: ActorMaterializer) extends Actor with ActorLogging {

  import context.dispatcher

  implicit val timeout = Timeout(15.seconds)

  val wordslist = ClusterSharding(context.system).start(
    typeName = WordsListSubTreeShardEntity.Name,
    entityProps = WordsListSubTreeShardEntity.props(mat),
    settings = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractShardId = WordsListSubTreeShardEntity.extractShardId,
    extractEntityId = WordsListSubTreeShardEntity.extractEntityId,
    allocationStrategy = new LeastShardAllocationStrategy(10, 3),
    handOffStopMessage = Stop)

  val homophones = ClusterSharding(context.system).start(
    typeName = HomophonesSubTreeShardEntity.Name,
    entityProps = HomophonesSubTreeShardEntity.props(mat),
    settings = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractShardId = HomophonesSubTreeShardEntity.extractShardId,
    extractEntityId = HomophonesSubTreeShardEntity.extractEntityId,
    allocationStrategy = new LeastShardAllocationStrategy(10, 3),
    handOffStopMessage = Stop)

  override def preStart = {
    log.info("************************** {}", self)
  }

  override def receive: Receive = {
    case search: SearchWord => (wordslist ? search).pipeTo(sender())
    case search: SearchHomophones => (homophones ? search).pipeTo(sender())

    //works only for local
    case (name: String, m @ ShardRegion.GetShardRegionState) =>
      name match {
        case WordsListSubTreeShardEntity.Name => (wordslist ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).mapTo[CurrentShardRegionState].pipeTo(sender())
      }

    case (name: String, m @ ShardRegion.GetCurrentRegions) =>
      name match {
        case WordsListSubTreeShardEntity.Name => (wordslist ? m).pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).pipeTo(sender())
      }

    case (name: String, m @ ShardRegion.GetClusterShardingStats(to)) =>
      name match {
        case WordsListSubTreeShardEntity.Name => (wordslist ? m).mapTo[ClusterShardingStats].pipeTo(sender())
        case HomophonesSubTreeShardEntity.Name => (homophones ? m).mapTo[ClusterShardingStats].pipeTo(sender())
      }
  }
}