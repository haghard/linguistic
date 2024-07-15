package linguistic.dao

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.Member
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.typed.SelfUp

import linguistic.Bootstrap

import java.lang.management.ManagementFactory
import java.time.LocalDateTime
import java.util.TimeZone
import scala.collection.immutable

object Guardian {

  sealed trait Protocol
  object Protocol {
    final case class SelfUpMsg(mba: immutable.SortedSet[Member]) extends Protocol
  }

  def apply(httpPort: Int, hostName: String, keypass: String, storepass: String): Behavior[Protocol] =
    Behaviors
      .setup[Protocol] { ctx ⇒
        implicit val system            = ctx.system
        implicit val cluster           = akka.cluster.typed.Cluster(system)
        implicit val selfUniqueAddress = SelfUniqueAddress(cluster.selfMember.uniqueAddress)

        cluster.subscriptions.tell(
          akka.cluster.typed.Subscribe(
            ctx.messageAdapter[SelfUp] {
              case m: SelfUp ⇒
                Protocol.SelfUpMsg(
                  scala.collection.immutable.SortedSet(m.currentClusterState.members.toSeq: _*)(Member.ageOrdering)
                )
            },
            classOf[SelfUp]
          )
        )

        Behaviors.receive[Protocol] {
          case (ctx, _ @Protocol.SelfUpMsg(membersByAge)) ⇒
            cluster.subscriptions ! akka.cluster.typed.Unsubscribe(ctx.self)

            import akka.cluster.Implicits._

            membersByAge.headOption.foreach { singleton ⇒
              val totalMemory = ManagementFactory
                .getOperatingSystemMXBean()
                .asInstanceOf[com.sun.management.OperatingSystemMXBean]
                .getTotalMemorySize()

              val rntm = sys.runtime
              val jvmInfo =
                s"Cores:${rntm.availableProcessors()} Memory:[Total=${rntm.totalMemory() / 1000000}Mb, Max=${rntm
                  .maxMemory() / 1000000}Mb, Free=${rntm.freeMemory() / 1000000}Mb, RAM=${totalMemory / 1000000} ]"

              //CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
              CassandraSessionExtension(ctx.system).session
              val config = ctx.system.settings.config
              val greeting =
                new StringBuilder()
                  .append('\n')
                  .append(
                    "================================================================================================="
                  )
                  .append('\n')
                  .append(
                    s"★ ★ ★  Environment: TimeZone: ${TimeZone.getDefault.getID} Started at ${LocalDateTime.now}  ★ ★ ★"
                  )
                  .append('\n')
                  .append(s"------------- Started: ${cluster.selfMember.details()}  ------------------")
                  .append("\n")
                  .append(
                    s" Singleton: [${singleton.addressWithIncNum()}] Leader:[${cluster.state.leader.getOrElse("")}]"
                  )
                  .append("\n")
                  .append(s"★ ★ ★  Akka cluster: ${config.getInt("akka.remote.artery.canonical.port")}  ★ ★ ★")
                  .append('\n')
                  .append(s"★ ★ ★  Akka seeds: ${config.getStringList("akka.cluster.seed-nodes")}  ★ ★ ★")
                  .append('\n')
                  .append(
                    s"★ ★ ★  Cassandra domain points: ${config.getStringList("datastax-java-driver.basic.contact-points")}  ★ ★ ★"
                  )
                  .append('\n')
                  .append(
                    s"★ ★ ★  Server online at http://${config.getString("akka.http.interface")}:$httpPort   ★ ★ ★"
                  )
                  .append('\n')
                  .append(
                    "================================================================================================="
                  )

              ctx.log.info(greeting.toString)

              Bootstrap(
                httpPort,
                hostName,
                config.getString("akka.http.ssl.keypass"),
                config.getString("akka.http.ssl.storepass")
              )(system, ctx)
            }

            Behaviors.same
        }
      }
      .narrow
}