package linguistic

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.actor.ActorSystem
import linguistic.ps._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import linguistic.protocol.SearchQuery
import linguistic.ps.btree.{BPlusTreeEntity, BPlusTreeFileEntity}
import linguistic.ps.pruningRadixTrie.PruningRadixTrieEntity

trait ShardingSupport {

  def startSharding(system: ActorSystem) = {

    val sharding = ClusterSharding(system)
    val settings = ClusterShardingSettings(system) // .withRememberEntities(true)

    /*val words = sharding.start(
      typeName = SuffixTreeEntity.Name,
      entityProps = SuffixTreeEntity.props(isPrefixBasedSearch = false),
      settings = settings,
      extractShardId = SuffixTreeEntity.extractShardId,
      extractEntityId = SuffixTreeEntity.extractEntityId
    )*/

    /*val words = sharding.start(
      typeName = SuffixTreeEntity22.Name,
      entityProps = SuffixTreeEntity22.props(isPrefixBasedSearch = false),
      settings = settings,
      extractShardId = SuffixTreeEntity22.extractShardId,
      extractEntityId = SuffixTreeEntity22.extractEntityId
    )*/

    /*val words = sharding.start(
      typeName = RadixTreeShardEntity.Name,
      //entityProps = SuffixTreeEntity2.props(globalMap),
      entityProps = RadixTreeShardEntity.props(),
      settings = settings,
      extractShardId = RadixTreeShardEntity.extractShardId,
      extractEntityId = RadixTreeShardEntity.extractEntityId
    )*/

    /*val words = sharding.start(
      typeName = PruningRadixTrieEntity.Name,
      entityProps = PruningRadixTrieEntity.props(),
      settings = settings,
      extractShardId = PruningRadixTrieEntity.extractShardId,
      extractEntityId = PruningRadixTrieEntity.extractEntityId
    )*/

    /*val words = sharding.start(
      typeName = BPlusTreeFileEntity.Name,
      entityProps = BPlusTreeFileEntity.props(),
      settings = settings,
      extractShardId = BPlusTreeFileEntity.extractShardId,
      extractEntityId = BPlusTreeFileEntity.extractEntityId
    )*/

    val words = sharding.start(
      typeName = BPlusTreeEntity.Name,
      entityProps = BPlusTreeEntity.props(),
      settings = settings,
      extractShardId = BPlusTreeEntity.extractShardId,
      extractEntityId = BPlusTreeEntity.extractEntityId
    )

    val homophones = sharding.start(
      typeName = HomophonesSubTreeShardEntity.Name,
      entityProps = HomophonesSubTreeShardEntity.props(),
      settings = settings,
      extractShardId = HomophonesSubTreeShardEntity.extractShardId,
      extractEntityId = HomophonesSubTreeShardEntity.extractEntityId
    )

    (words.toTyped[SearchQuery], homophones)
  }
}
