package linguistic.api

import akka.http.scaladsl.model.ContentTypes.`text/html(UTF-8)`
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.HttpResponse
import akka.util.ByteString
import linguistic.js.AppScript

final class WebAssets extends akka.http.scaladsl.server.Directives {
  import scala.concurrent.duration._
  implicit val askTimeout = akka.util.Timeout(3.seconds)

  val route =
    extractMaterializer { implicit mat =>
      extractExecutionContext { implicit ec =>
        extractLog { _ =>
          pathSingleSlash {
            get {
              complete {
                HttpResponse(entity = Strict(`text/html(UTF-8)`,
                  ByteString(AppScript().render)))
              }
            }
          } ~ {
            pathPrefix("assets" / Remaining) { file =>
              encodeResponse(getFromResource("public/" + file))
            }
          }
        }
      }
    }
}

