package linguistic.api

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.cluster.metrics.StandardMetrics.{Cpu, HeapMemory}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsEvent, ClusterMetricsExtension}
import akka.stream.BoundedSourceQueue
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.netty.util.internal.ThreadLocalRandom
import linguistic.ps.cassandraBPlusTree.HeapUtils
import spray.json.{JsObject, JsString}

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

object ClusteredJvmMetrics {

  sealed trait Metrics extends ClusterMetricsEvent
  object Metrics {
    final case class ClusterMetrics(bs: ByteString) extends Metrics
    final case object MetricsError                  extends Metrics
    final case object Complete                      extends Metrics
  }

  def apply(
    output: BoundedSourceQueue[ByteString]
  ): Behavior[ClusterMetricsEvent] =
    Behaviors.setup { ctx =>
      val divider   = 1024 * 1024
      val defaultTZ = ZoneId.of(java.util.TimeZone.getDefault.getID)
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")
      val ex        = ClusterMetricsExtension(ctx.system)
      ex.subscribe(ctx.self.toClassic)
      active(output, ex, divider, defaultTZ, formatter)
    }

  def active(
    outbound: BoundedSourceQueue[ByteString],
    ex: ClusterMetricsExtension,
    divider: Long,
    defaultTZ: ZoneId,
    formatter: DateTimeFormatter
  ): Behavior[ClusterMetricsEvent] =
    Behaviors
      .receive[ClusterMetricsEvent] {
        case (ctx, _ @ClusterMetricsChanged(clusterMetrics)) =>
          clusterMetrics.foreach {
            // case Cpu(addr, ts, sla, cpuCmb, cpuStolen, proc) =>
            case HeapMemory(address, timestamp, used, _, max) =>
              val now = formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), defaultTZ))
              val json = JsObject(
                Map(
                  "node"   -> JsString(address.toString),
                  "metric" -> JsString("heap"),
                  "when"   -> JsString(now),
                  "used"   -> JsString((used / divider).toString + " mb"),
                  "max"    -> JsString((max.getOrElse(0L) / divider).toString + " mb")
                )
              ).prettyPrint

              if (ThreadLocalRandom.current().nextDouble() > .75) {
                println(
                  s"""
                    |***********************$now******************************
                    |${HeapUtils.logNativeMemory()}
                    |*****************************************************
                    |""".stripMargin
                )
              }
              outbound.offer(ByteString(json))
            case other =>
              ctx.log.warn("unexpected metric: {}", other.getClass.getName)
          }
          Behaviors.same
        case (ctx, other) =>
          ctx.log.warn("Unexpected metric: {}", other.getClass.getName)
          Behaviors.ignore
      }
      .receiveSignal { case (ctx, PostStop) =>
        ex.unsubscribe(ctx.self.toClassic)
        Behaviors.stopped
      }

  def metricsStream(src: Source[ByteString, NotUsed], clientId: Long)(implicit
    sys: ActorSystem[_]
  ): Source[ByteString, NotUsed] =
    src
      .watchTermination() { (_, done) =>
        done.onComplete(_ => println(s"Disconnected $clientId"))(sys.executionContext)
        NotUsed
      }
      .map { bts =>
        println(s" -> $clientId")
        bts
      }

  /*
  def jvmSource(metrics: ActorRef[ClusterMetricsEvent])(implicit sys: ActorSystem[_]): Source[ByteString, NotUsed] = {

    val src: Source[ByteString, NotUsed] =
      ActorSource
        .actorRef[Metrics](
          completionMatcher = { case Metrics.Complete => CompletionStrategy.immediately },
          failureMatcher = { case Metrics.MetricsError => new Exception("JvmMetrics failure") },
          bufferSize = 1 << 2,
          overflowStrategy = OverflowStrategy.dropTail
        )
        .collect { case Metrics.ClusterMetrics(json) => json }
        .mapMaterializedValue { streamRef =>
          sys.systemActorOf(apply(streamRef), "jvm")
          NotUsed
        }
        //.toMat(BroadcastHub.sink[ByteString](4))(Keep.right)
        //.toMat(sink0)(Keep.both)
        .withAttributes(
          ActorAttributes.supervisionStrategy { case NonFatal(ex) =>
            sys.log.error("ClusteredJvmMetrics error!", ex)
            Supervision.Stop
          }
        )
        .run()

    src.runWith(Sink.ignore)

    src.runWith(sink0)
    src
  }*/
}
