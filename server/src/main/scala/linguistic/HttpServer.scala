package linguistic

import linguistic.Application._
import linguistic.dao.UsersRepo
import akka.cluster.sharding.ClusterSharding
import akka.stream.ActorMaterializerSettings
import akka.actor.{Actor, ActorIdentity, ActorLogging, Identify, Props, Status}
import linguistic.utils.ShutdownCoordinator
import ShutdownCoordinator.NodeShutdownOpts
import linguistic.ps.{HomophonesSubTreeShardEntity, WordShardEntity}

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
  import akka.pattern.pipe
  import akka.http.scaladsl.server.RouteResult._
  import akka.http.scaladsl.server.RouteConcatenation._

  implicit val system = context.system
  implicit val ex = system.dispatchers.lookup(HttpDispatcher)
  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings.create(coreSystem)
      .withDispatcher(HttpDispatcher))(system)

  startRegions(system, mat)

  val wordShard = ClusterSharding(coreSystem).shardRegion(WordShardEntity.Name)
  val homophonesShard = ClusterSharding(coreSystem).shardRegion(HomophonesSubTreeShardEntity.Name)
  val regions = scala.collection.immutable.Set(wordShard, homophonesShard)

  val users = system.actorOf(UsersRepo.props, "users")

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

  import scala.concurrent.duration._
  def serverBinding(b: akka.http.scaladsl.Http.ServerBinding) = {
    log.info("Binding on {}",  b.localAddress)

    import akka.pattern.ask
    implicit val t = akka.util.Timeout(3 seconds)

    wordShard.ask(Identify(1)).mapTo[ActorIdentity].flatMap { ident =>
      if(ident.correlationId == 1) {
        homophonesShard.ask(Identify(2)).mapTo[ActorIdentity].map { ident =>
          if (ident.correlationId == 2) users ! UsersRepo.Activate
          else throw new Exception(s"Couldn't start homophones-shard ${ident.correlationId}")
        }
      } else throw new Exception(s"Couldn't start words-shard ${ident.correlationId}")
    }

    //https://gist.github.com/nelanka/891e9ac82fc83a6ab561
    ShutdownCoordinator.register(NodeShutdownOpts(5 seconds, 20 seconds), self, regions)(coreSystem)
    context become bound(b)
  }

  def handleBindFailure(cause: Throwable) = {
    log.error(cause, s"Can't bind to $address:$port")
    context stop self
  }

  def bound(b: akka.http.scaladsl.Http.ServerBinding): Receive = {
    case HttpServer.Stop =>
      log.info("Unbound {}:{}", address, port)
      b.unbind().onComplete { _ =>  mat.shutdown }
  }
}