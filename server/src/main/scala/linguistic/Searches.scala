package linguistic

import akka.cluster.sharding.ShardRegion
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import linguistic.protocol.{AddOneWord, SearchQuery}
import linguistic.ps.{HomophonesSubTreeShardEntity, RadixTreeShardEntity}

object Searches {

  def props(wordslist: ActorRef, homophones: ActorRef) =
    Props(new Searches(wordslist, homophones)).withDispatcher("shard-dispatcher")
}

class Searches(terms: ActorRef, homophones: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case search: SearchQuery.WordsQuery =>
      terms forward search
    case search: SearchQuery.HomophonesQuery =>
      homophones forward search
    case add: AddOneWord =>
      terms forward add

    // works only for local
    case (name: String, m @ ShardRegion.GetShardRegionState) =>
      name match {
        case RadixTreeShardEntity.Name =>
          terms forward m
        case HomophonesSubTreeShardEntity.Name =>
          homophones forward m
      }

    case (name: String, m @ ShardRegion.GetCurrentRegions) =>
      name match {
        case RadixTreeShardEntity.Name =>
          terms forward m
        case HomophonesSubTreeShardEntity.Name =>
          homophones forward m
        case _ => //
      }

    case (name: String, m @ ShardRegion.GetClusterShardingStats) =>
      name match {
        case RadixTreeShardEntity.Name =>
          terms forward m
        case HomophonesSubTreeShardEntity.Name =>
          homophones forward m
        case _ => //
      }
  }
}
