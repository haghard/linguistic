package linguistic
import akka.stream.ActorMaterializer
import akka.cluster.sharding.ShardRegion
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import linguistic.protocol.{HomophonesQuery, WordsQuery}
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}

object Searches {
  def props(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) =
    Props(new Searches(mat, wordslist, homophones))
      .withDispatcher("shard-dispatcher")
}

class Searches(mat: ActorMaterializer, wordslist: ActorRef, homophones: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case search: WordsQuery =>
      wordslist forward search
    case search: HomophonesQuery =>
      homophones forward search

    //works only for local
    case (name: String, m @ ShardRegion.GetShardRegionState) =>
      name match {
        case WordShardEntity.Name =>
          wordslist forward  m
        case HomophonesSubTreeShardEntity.Name =>
          homophones forward  m
      }

    case (name: String, m @ ShardRegion.GetCurrentRegions) =>
      name match {
        case WordShardEntity.Name =>
          (wordslist forward  m)
        case HomophonesSubTreeShardEntity.Name =>
          (homophones forward  m)
        case _ => //
      }

    case (name: String, m @ ShardRegion.GetClusterShardingStats) =>
      name match {
        case WordShardEntity.Name =>
          wordslist forward  m
        case HomophonesSubTreeShardEntity.Name =>
          homophones forward m
        case _ => //
      }
  }
}