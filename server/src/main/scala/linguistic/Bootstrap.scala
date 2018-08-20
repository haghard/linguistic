package linguistic

import linguistic.dao.Accounts
import linguistic.dao.Accounts.Activate
import akka.stream.ActorMaterializerSettings
import akka.actor.{Actor, ActorLogging, ActorRef, CoordinatedShutdown, Props, Status}
import Bootstrap._
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.server.RouteResult._
import akka.pattern.pipe
import linguistic.api.WebAssets
import linguistic.protocol.{HomophonesQuery, WordsQuery}

object Bootstrap {

  case object InitSharding

  val HttpDispatcher = "akka.http.dispatcher"

  def props(port: Int, address: String, keypass: String, storepass: String) =
    Props(new Bootstrap(port, address, keypass, storepass)).withDispatcher(HttpDispatcher)
}

class Bootstrap(port: Int, address: String, keypass: String, storepass: String) extends Actor
  with ActorLogging with SslSupport with ShardingSupport {

  implicit val system = context.system
  implicit val ex = system.dispatchers.lookup(HttpDispatcher)
  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings.create(system).withDispatcher(HttpDispatcher))(system)

  override def preStart() =
    self ! InitSharding

  def idle: Receive = {
    case InitSharding =>
      val (wordRegion, homophonesRegion) = startSharding(system)
      val users = system.actorOf(Accounts.props, "users")
      val search = system.actorOf(Searches.props(mat, wordRegion, homophonesRegion), "search")

      val routes = new WebAssets().route ~ new api.SearchApi(search).route ~
        new api.UsersApi(users).route ~ new api.ClusterApi(self, search,
        scala.collection.immutable.Set(wordRegion, homophonesRegion)).route

      Http()
        .bindAndHandle(routes, address, port, connectionContext = https(keypass, storepass))
        .pipeTo(self)

      context.become(awaitHttpBinding(users, search))
  }

  def awaitHttpBinding(users: ActorRef, search: ActorRef): Receive = {
    case b: ServerBinding =>
      log.info("Binding on {}", b.localAddress)
      //warm up
      users ! Activate
      search ! WordsQuery("average", 1)
      search ! HomophonesQuery("rose", 1)
      
      //https://discuss.lightbend.com/t/graceful-termination-on-sigterm-using-akka-http/1619
      CoordinatedShutdown(system).addTask(
        CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
        b.unbind()
      }
      context.become(warmUp(b))

    case Status.Failure(ex) =>
      log.error(ex, s"Can't bind to $address:$port")
      context stop self
  }

  def warmUp(bind: ServerBinding): Receive = {
    case Status.Failure(ex) =>
      log.error(ex, "Warm up error")
      bind.unbind().foreach(_ => context stop self)
    case _ =>
  }

  override def receive = idle
}