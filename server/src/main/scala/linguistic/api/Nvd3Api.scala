package linguistic.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.{ContentTypes, HttpResponse}
import akka.http.scaladsl.server._
import akka.util.ByteString

import scala.concurrent.duration._

class Nvd3Api(implicit sys: ActorSystem) extends Directives {
  import linguistic._
  implicit val dispatcher = sys.dispatchers.lookup("akka.http.dispatcher")

  val route =
    extractMaterializer { implicit mat =>
      extractExecutionContext { implicit ec =>
        extractLog { log =>
          path("graph") {
            get(complete(linguistic.html.graphTemplate.render("graph")))
          } ~ path("bar-graph") {
            complete(HttpResponse(entity = Strict(ContentTypes.`text/html(UTF-8)`,
              ByteString(BarGraphComponent("bar-graph").render))))
            //play way
            //complete(geolocation.html.graphTemplate.render("graph"))
          } ~ path("histogram") {
            complete(HttpResponse(entity = Strict(ContentTypes.`text/html(UTF-8)`,
              ByteString(HistogramComponent("histogram").render))))
          }
        }
      }
    }
}
