package linguistic.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity.{Chunked, Strict}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.pattern.ask
import akka.util.ByteString
import linguistic.AuthTokenSupport
import linguistic.WordsSearchProtocol.{Search, SearchHomophones, SearchResults, SearchWord}
import linguistic.js.AppScript

import scala.concurrent.duration._

final class SearchApi(search: ActorRef)(implicit val system: ActorSystem) extends BaseApi
  with AuthTokenSupport {
  implicit val askTimeout = akka.util.Timeout(7.seconds)
  implicit val ec = system.dispatchers.lookup("akka.http.dispatcher")

  //withRequestTimeout(usersTimeout) {

  /*((pathPrefix("assets" / Remaining) & respondWithHeader(`Cache-Control`(`no-cache`)))) { file =>
              // optionally compresses the response with Gzip or Deflate
              // if the client accepts compressed responses
              getFromResource("public/" + file)
   }*/

  //http --verify=no https://192.168.0.62:9443/api/v1.0/wordslist/search"?q=aa"
  val route =
    extractMaterializer { implicit mat =>
      //extractExecutionContext { implicit ec =>
      extractLog { _ =>
        pathSingleSlash {
          get {
            complete {
              HttpResponse(entity = Strict(ContentTypes.`text/html(UTF-8)`,
                ByteString(AppScript().render)))
              //linguistic.html.indexTemplate.render("main")
            }
          }
        } ~ path("chat") {
          getFromResource("web/chat-demo.html")
          //getFromResourceDirectory("web")
        } ~ {
          pathPrefix("assets" / Remaining) { file =>
            encodeResponse(getFromResource("public/" + file))
          }
        } ~ pathPrefix(apiPrefix) {
            get {
              path(Segment / shared.Routes.search) { seq =>
                requiredHttpSession(ec) { session ⇒
                  parameters('q.as[String], 'n ? 30) { (q, max) =>
                    complete {
                      val searchQ = seq match {
                        case shared.Routes.searchWordsPath => SearchWord(q, max)
                        case shared.Routes.searchHomophonesPath => SearchHomophones(q, max)
                      }
                      searchDomain(searchQ).map { res =>
                        HttpResponse(entity = Chunked.fromData(ContentTypes.`text/plain(UTF-8)`,
                          chunks = res.source.map(word => ByteString(s"$word,"))))
                      }
                    }
                  }
                }
              }
            }
        }
      }
    }

  def searchDomain(q: Search) = (search ? q).mapTo[SearchResults]
}