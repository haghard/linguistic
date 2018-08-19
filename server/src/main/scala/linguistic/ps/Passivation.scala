package linguistic.ps

import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.Passivate
import scala.concurrent.duration._

trait Passivation extends ActorLogging {
  this: Actor =>

  val passivationTimeout = 15.minutes

  protected def passivate(receive: Receive): Receive = receive.orElse {
    case ReceiveTimeout =>
      log.info(s"passivate: {}", self)
      context.parent ! Passivate(stopMessage = PoisonPill)

    case PoisonPill => context stop self
  }
}
