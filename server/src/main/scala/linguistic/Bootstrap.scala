package linguistic

import linguistic.dao.Accounts
import linguistic.dao.Accounts.Activate
import akka.stream.ActorMaterializerSettings
import akka.actor.{Actor, ActorLogging, ActorRef, CoordinatedShutdown, Props, Status}
import Bootstrap._
import akka.Done
import akka.actor.CoordinatedShutdown.{PhaseServiceRequestsDone, PhaseServiceUnbind, Reason}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.RouteResult._
import akka.pattern.pipe
import linguistic.api.WebAssets
import linguistic.protocol.{HomophonesQuery, WordsQuery}

import scala.concurrent.Promise
import scala.concurrent.duration._

object Bootstrap {

  final private case object InitSharding
  final private case object BindFailure extends Reason

  val HttpDispatcher = "akka.http.dispatcher"

  def props(port: Int, address: String, keypass: String, storepass: String) =
    Props(new Bootstrap(port, address, keypass, storepass)).withDispatcher(HttpDispatcher)
}

class Bootstrap(port: Int, address: String, keypass: String, storepass: String)
    extends Actor
    with ActorLogging
    with SslSupport
    with ShardingSupport {

  implicit val system = context.system
  implicit val ex     = system.dispatchers.lookup(HttpDispatcher)
  implicit val mat =
    akka.stream.ActorMaterializer(ActorMaterializerSettings.create(system).withDispatcher(HttpDispatcher))(system)

  val shutdown = CoordinatedShutdown(system)

  override def preStart() =
    self ! InitSharding

  def idle: Receive = {
    case InitSharding =>
      val (wordRegion, homophonesRegion) = startSharding(system)
      val users                          = context.actorOf(Accounts.props, "users")
      val search                         = context.actorOf(Searches.props(mat, wordRegion, homophonesRegion), "search")

      val routes = new WebAssets().route ~ new api.SearchApi(search).route ~
        new api.UsersApi(users).route ~ new api.ClusterApi(
          self,
          search,
          scala.collection.immutable.Set(wordRegion, homophonesRegion)
        ).route

      Http()
        .bindAndHandle(routes, address, port, connectionContext = https(keypass, storepass))
        .pipeTo(self)

      context.become(awaitBinding(users, search))
  }

  def awaitBinding(users: ActorRef, searchShardRegion: ActorRef): Receive = {
    case b: ServerBinding =>
      //warm up search
      users ! Activate
      searchShardRegion ! WordsQuery("average", 1)
      searchShardRegion ! HomophonesQuery("rose", 1)

      shutdown.addTask(PhaseServiceUnbind, "api.unbind") { () ⇒
        log.info("api.unbind")
        // No new connections are accepted
        // Existing connections are still allowed to perform request/response cycles
        b.terminate(3.seconds).map(_ ⇒ Done)
      }

      shutdown.addTask(PhaseServiceRequestsDone, "requests-done") { () ⇒
        log.info("requests-done")
        // Wait 2 seconds until all HTTP requests have been processed
        val p = Promise[Done]()
        system.scheduler.scheduleOnce(2.seconds) {
          p.success(Done)
        }
        p.future
      }

    case Status.Failure(ex) =>
      log.error(ex, s"Can't bind to $address:$port")
      //graceful stop logic
      shutdown.run(Bootstrap.BindFailure)
  }

  override def receive = idle
}
