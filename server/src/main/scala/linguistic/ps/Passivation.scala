package linguistic.ps

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.Passivate

trait Passivation extends ActorLogging {
  this: Actor =>

  protected def passivate(receive: Receive): Receive = receive.orElse {
    case ReceiveTimeout =>
      log.info(s"passivate: {}", self)
      context.parent ! Passivate(stopMessage = PoisonPill)

    case PoisonPill => context stop self
  }
}
