package linguistic

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

trait ShardingSupport {

  def startRegions(system: ActorSystem, mat: ActorMaterializer) = {

    ClusterSharding(system).start(
      typeName = WordShardEntity.Name,
      entityProps = WordShardEntity.props(mat),
      settings = ClusterShardingSettings(system),
      extractShardId = WordShardEntity.extractShardId,
      extractEntityId = WordShardEntity.extractEntityId)

    ClusterSharding(system).start(
      typeName = HomophonesSubTreeShardEntity.Name,
      entityProps = HomophonesSubTreeShardEntity.props(mat),
      settings = ClusterShardingSettings(system),
      extractShardId = HomophonesSubTreeShardEntity.extractShardId,
      extractEntityId = HomophonesSubTreeShardEntity.extractEntityId)
      /*
      allocationStrategy = new LeastShardAllocationStrategy(3, 2),
      handOffStopMessage = Stop)
      */
  }
}