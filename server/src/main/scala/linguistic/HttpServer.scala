package linguistic

import akka.actor.{Actor, ActorLogging, Props, Status}
import akka.cluster.Cluster
import akka.cluster.sharding.ClusterSharding
import akka.stream.ActorMaterializerSettings
import linguistic.Application._
import linguistic.WordsSearchProtocol.{SearchHomophones, SearchWord}
import linguistic.dao.Accounts
import linguistic.dao.Accounts.Activate
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}
import linguistic.utils.ShutdownCoordinator
import linguistic.utils.ShutdownCoordinator.NodeShutdownOpts

object HttpServer {
  val HttpDispatcher = "akka.http.dispatcher"

  object Stop

  def props(port: Int, address: String, keypass: String, storepass: String) =
    Props(new HttpServer(port, address, keypass, storepass)).withDispatcher(HttpDispatcher)
}

class HttpServer(port: Int, address: String, keypass: String, storepass: String) extends Actor with ActorLogging
  with SslSupport with ShardingSupport {

  import HttpServer._
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.server.RouteConcatenation._
  import akka.http.scaladsl.server.RouteResult._
  import akka.pattern.pipe

  implicit val system = context.system
  implicit val ex = system.dispatchers.lookup(HttpDispatcher)
  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings.create(coreSystem)
      .withDispatcher(HttpDispatcher))(system)

  startRegions(system, mat)

  val wordShard = ClusterSharding(coreSystem).shardRegion(WordShardEntity.Name)
  val homophonesShard = ClusterSharding(coreSystem).shardRegion(HomophonesSubTreeShardEntity.Name)
  val regions = scala.collection.immutable.Set(wordShard, homophonesShard)

  val users = system.actorOf(Accounts.props, "users")

  val searchMaster = system.actorOf(SearchMaster.props(mat, wordShard, homophonesShard), name = "search-master")

  val routes = new api.SearchApi(searchMaster).route ~ new api.UsersApi(users).route ~
    new api.ClusterApi(self, searchMaster, regions).route

  Http()
    .bindAndHandle(routes, address, port, connectionContext = https(keypass, storepass))
    .pipeTo(self)

  override def receive = {
    case b: akka.http.scaladsl.Http.ServerBinding => serverBinding(b)
    case Status.Failure(c) => handleBindFailure(c)
  }

  def serverBinding(b: akka.http.scaladsl.Http.ServerBinding) = {
    log.info("Binding on {}", b.localAddress)

    searchMaster ! SearchWord("average", 1)
    homophonesShard ! SearchHomophones("rose", 1)
    users ! Activate

    //https://gist.github.com/nelanka/891e9ac82fc83a6ab561
    ShutdownCoordinator.register(NodeShutdownOpts(), self, regions)(coreSystem)

    //ClusterHttpManagement(Cluster(coreSystem))
    context become bound(b)
  }

  def handleBindFailure(cause: Throwable) = {
    log.error(cause, s"Can't bind to $address:$port")
    context stop self
  }

  def bound(b: akka.http.scaladsl.Http.ServerBinding): Receive = {
    case HttpServer.Stop =>
      log.info("Unbound {}:{}", address, port)
      b.unbind().onComplete { _ => mat.shutdown }
  }
}