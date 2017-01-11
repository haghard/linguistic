package linguistic.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.ByteString
import linguistic.AuthTokenSupport
import linguistic.WordsSearchProtocol.{Search, SearchHomophones, SearchWord, SearchResults}

import scala.concurrent.duration._

final class SearchApi(search: ActorRef)(implicit val system: ActorSystem) extends BaseApi
  with AuthTokenSupport {
  import linguistic._
  implicit val askTimeout = akka.util.Timeout(8 seconds)
  implicit val ec = system.dispatchers.lookup("akka.http.dispatcher")

  //withRequestTimeout(usersTimeout) {
  val route =
    extractMaterializer { implicit mat =>
      //extractExecutionContext { implicit ec =>
      extractLog { log =>
        pathSingleSlash {
          get(complete(linguistic.html.indexTemplate.render("main")))
        } ~ path("chat") {
          getFromResource("web/chat-demo.html")
          //getFromResourceDirectory("web")
        } ~
          pathPrefix("assets" / Remaining) { file =>
            encodeResponse {
              getFromResource("public/" + file)
            }
          } ~ pathPrefix(apiPrefix) {
            get {
              path(Segment / shared.Routes.search) { seq =>
                requiredHttpSession(ec) { session ⇒
                  parameters('q.as[String], 'n ? 50) { (q, max) =>
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