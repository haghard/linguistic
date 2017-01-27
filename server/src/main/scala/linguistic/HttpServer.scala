package linguistic

import linguistic.Application._
import linguistic.dao.UsersRepo
import akka.cluster.sharding.ClusterSharding
import akka.stream.ActorMaterializerSettings
import akka.actor.{Actor, ActorLogging, ActorSystem, Status}
import linguistic.utils.ShutdownCoordinator
import ShutdownCoordinator.NodeShutdownOpts
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}

object HttpServer {
  val HttpDispatcher = "akka.http.dispatcher"
  object Stop
}

class HttpServer(port: Int, address: String, keypass: String, storepass: String) extends Actor with ActorLogging
  with SslSupport with ShardingSupport {
  import HttpServer._
  import akka.http.scaladsl.Http
  import akka.pattern.pipe
  import akka.http.scaladsl.server.RouteResult._
  import akka.http.scaladsl.server.RouteConcatenation._

  implicit val system = context.system
  implicit val ex = system.dispatchers.lookup(HttpDispatcher)
  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings.create(coreSystem).withDispatcher(HttpDispatcher)
  )(system)

  //context.system.actorOf(ClusterListener.props, "listener")

  startRegions(system, mat)

  val wordShard = ClusterSharding(coreSystem).shardRegion(WordShardEntity.Name)
  val homophonesShard = ClusterSharding(coreSystem).shardRegion(HomophonesSubTreeShardEntity.Name)
  val regions = scala.collection.immutable.Set(wordShard, homophonesShard)
  val searchMaster = system.actorOf(SearchMaster.props(mat, wordShard, homophonesShard), name = "search-master")

  val routes = new api.SearchApi(searchMaster).route ~ new api.Nvd3Api().route ~
    new api.UsersApi(new UsersRepo()).route  ~ new api.ClusterApi(self, searchMaster, regions).route

  Http()
    .bindAndHandle(routes, address, port, connectionContext = https(keypass, storepass))
    .pipeTo(self)

  override def receive = {
    case b: akka.http.scaladsl.Http.ServerBinding => serverBinding(b)
    case Status.Failure(c) => handleBindFailure(c)
  }

  def serverBinding(b: akka.http.scaladsl.Http.ServerBinding) = {
    log.info("Binding on {}",  b.localAddress)

    //https://gist.github.com/nelanka/891e9ac82fc83a6ab561
    import scala.concurrent.duration._
    ShutdownCoordinator.register(NodeShutdownOpts(5 seconds, 20 seconds), self, regions)(coreSystem)
    context become bound(b)
  }

  def handleBindFailure(cause: Throwable) = {
    log.error(cause, s"Can't bind to $address:$port!")
    (context stop self)
  }

  def bound(b: akka.http.scaladsl.Http.ServerBinding): Receive = {
    case HttpServer.Stop =>
      log.info("Unbound {}:{}", address, port)
      b.unbind().onComplete { _ =>
        mat.shutdown()
      }
  }
}