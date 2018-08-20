package linguistic.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.ByteString
import linguistic.AuthTokenSupport
import linguistic.WordsSearchProtocol.SearchResults

import scala.concurrent.duration._
import ContentTypes._
import linguistic.protocol.{HomophonesQuery, SearchQuery, WordsQuery}

import scala.collection.immutable
import scala.concurrent.ExecutionContext

final class SearchApi(search: ActorRef)(implicit val system: ActorSystem) extends BaseApi
  with AuthTokenSupport {
  implicit val askTimeout = akka.util.Timeout(5.seconds)

  //withRequestTimeout(usersTimeout) {

  /*
    import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`
    import akka.http.scaladsl.model.headers.`Cache-Control`
    ((pathPrefix("assets" / Remaining) & respondWithHeader(`Cache-Control`(`no-cache`)))) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      getFromResource("public/" + file)
    }
  */

  //http --verify=no https://192.168.0.62:9443/api/v1.0/words/search"?q=aa"
  val route =
    extractMaterializer { implicit mat =>
      extractLog { _ =>
        pathPrefix(apiPrefix) {
          get {
            path(Segment / shared.Routes.search) { seq =>
              requiredHttpSession(mat.executionContext) { _ ⇒
                parameters('q.as[String], 'n ? 30) { (q, limit) =>
                  complete {
                    if (q.isEmpty)
                      HttpResponse(entity = Chunked.fromData(`text/plain(UTF-8)`,
                        chunks = SearchResults(immutable.Seq.empty[String]).source.map(ByteString(_))))
                    else {
                      val searchQ = seq match {
                        case shared.Routes.searchWordsPath => WordsQuery(q, limit)
                        case shared.Routes.searchHomophonesPath => HomophonesQuery(q, limit)
                      }
                      runSearch(searchQ)(mat.executionContext).map { res =>
                        HttpResponse(entity = Chunked.fromData(`text/plain(UTF-8)`,
                          chunks = res.source.map(word => ByteString(s"$word,"))))
                      }(mat.executionContext)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  private def runSearch(q: SearchQuery)(implicit ex: ExecutionContext) =
    ((search ? q) (askTimeout)).mapTo[SearchResults]
}