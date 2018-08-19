package linguistic

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

trait ShardingSupport {

  def startSharding(system: ActorSystem)(implicit mat: ActorMaterializer) = {

    val words = ClusterSharding(system).start(
      typeName = WordShardEntity.Name,
      entityProps = WordShardEntity.props(mat),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractShardId = WordShardEntity.extractShardId,
      extractEntityId = WordShardEntity.extractEntityId)

    val homophones = ClusterSharding(system).start(
      typeName = HomophonesSubTreeShardEntity.Name,
      entityProps = HomophonesSubTreeShardEntity.props(mat),
      settings = ClusterShardingSettings(system).withRememberEntities(true),
      extractShardId = HomophonesSubTreeShardEntity.extractShardId,
      extractEntityId = HomophonesSubTreeShardEntity.extractEntityId)

    (words, homophones)
  }
}