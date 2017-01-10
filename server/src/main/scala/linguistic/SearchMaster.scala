package linguistic

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.sharding.{ShardRegion, ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardCoordinator.LeastShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.{ClusterShardingStats, CurrentShardRegionState}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import linguistic.WordsSearchProtocol.{SearchHomophones, SearchWord}
import linguistic.ps.{HomophonesSubTreeShardEntity, WordsListSubTreeShardEntity}
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}

object SearchMaster {
  def props(mat: ActorMaterializer) =
    Props(new SearchMaster(mat)).withDispatcher("shard-dispatcher")
}

class SearchMaster(mat: ActorMaterializer) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val timeout = Timeout(15.seconds)

  val wordslist = ClusterSharding(context.system).start(
    typeName = "wordslist",
    entityProps = WordsListSubTreeShardEntity.props(mat),
    settings = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractShardId = WordsListSubTreeShardEntity.extractShardId,
    extractEntityId = WordsListSubTreeShardEntity.extractEntityId,
    allocationStrategy = new LeastShardAllocationStrategy(10, 3),
    handOffStopMessage = Stop)

  val homophones = ClusterSharding(context.system).start(
      typeName = "homophones",
      entityProps = HomophonesSubTreeShardEntity.props(mat),
      settings = ClusterShardingSettings(context.system).withRememberEntities(true),
      extractShardId = HomophonesSubTreeShardEntity.extractShardId,
      extractEntityId = HomophonesSubTreeShardEntity.extractEntityId,
      allocationStrategy = new LeastShardAllocationStrategy(10, 3),
      handOffStopMessage = Stop)

  override def receive: Receive = {
    case search: SearchWord => (wordslist ? search).pipeTo(sender())
    case search: SearchHomophones => (homophones ? search).pipeTo(sender())

    //works only for local
    case s @ ShardRegion.GetShardRegionState =>
      (homophones ? s).mapTo[CurrentShardRegionState].pipeTo(sender())

    case s @ ShardRegion.GetCurrentRegions =>
      (homophones ? ShardRegion.GetCurrentRegions).pipeTo(sender())

    case s @ ShardRegion.GetClusterShardingStats(to) =>
      (homophones ? s).mapTo[ClusterShardingStats].pipeTo(sender())
  }
}