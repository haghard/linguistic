package linguistic.utils

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberEvent

object ClusterListener {
  def props = Props(new ClusterListener)
}

class ClusterListener extends Actor with ActorLogging {

  Cluster(context.system).subscribe(self, classOf[MemberEvent])

  override def receive: Receive = {
    case e: MemberEvent =>
      log.info("********************{}**********", e)
  }
}
