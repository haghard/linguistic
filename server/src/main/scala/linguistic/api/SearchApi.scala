package linguistic.api

import akka.actor.typed.ActorRef
import akka.actor.CoordinatedShutdown
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model._
import akka.util.ByteString
import linguistic.AuthTokenSupport

import scala.concurrent.duration._
import ContentTypes._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.stream.{ActorAttributes, Attributes, QueueCompletionResult, QueueOfferResult, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import linguistic.api.SearchApi._
import linguistic.protocol._
import org.slf4j.Logger

import scala.collection.immutable
import scala.concurrent.{Future, Promise}
import scala.util.control.{NoStackTrace, NonFatal}

object SearchApi {
  final case class SearchTimeout(name: String, timeout: FiniteDuration)
      extends Exception(s"$name. No response within $timeout")
      with NoStackTrace

  def resume(cause: Throwable)(logger: Logger): Supervision.Directive = {
    logger.warn(s"SearchApifailed and resumes {}", cause)
    Supervision.Resume
  }
}

final class SearchApi(
  searchRegion: ActorRef[SearchQuery]
)(implicit val system: akka.actor.typed.ActorSystem[_])
    extends BaseApi
    with AuthTokenSupport {

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

  import system.executionContext
  implicit val sch = system.scheduler
  val logger       = system.log

  implicit val timeout = akka.util.Timeout(3.seconds)

  val parallelism = 3
  val bufferSize  = 1 << 6

  val (queue, done) =
    Source
      .queue[(SearchQuery, Promise[Option[SearchResults]])](bufferSize /*, OverflowStrategy.backpressure*/ )
      .via(
        Flow[(SearchQuery, Promise[Option[SearchResults]])]
          .withAttributes(Attributes.inputBuffer(0, 0))
          .mapAsyncUnordered(parallelism) {
            case (q, p) ⇒
              q match {
                case SearchQuery.HomophonesQuery(keyword, maxResults, replyTo) ⇒
                  ???
                case sw: SearchQuery.WordsQuery ⇒
                  searchRegion
                    .ask[SearchResults](replyTo0 ⇒ sw.copy(replyTo = replyTo0))
                    .map(r ⇒ (Some(r), p))
                    .recover[(Option[SearchResults], Promise[Option[SearchResults]])] {
                      case NonFatal(_) ⇒ (None, p)
                    }
              }
          }
          .named("location-tracker-api")
      )
      .toMat(Sink.foreach[(Option[SearchResults], Promise[Option[SearchResults]])] {
        case (response, p) ⇒ p.trySuccess(response)
      })(
        Keep.both
      )
      .addAttributes(ActorAttributes.supervisionStrategy(resume(_)(logger)))
      .run()

  CoordinatedShutdown(system)
    .addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "shutdown-searchApi") { () ⇒
      logger.info(s"★ ★ ★ CoordinatedShutdown [SearchApi.shutdown]  ★ ★ ★")
      queue.complete()
      done
    }

  // to build full index
  ('a' to 'z').foreach { letterA ⇒
    queue.offer((SearchQuery.WordsQuery(letterA.toString, 1, null), Promise[Option[SearchResults]]()))
  }

  //http --verify=no https://120.0.0.1:9443/api/v1.0/words/search"?q=aa"
  //http 127.0.0.1:9443/api/v1.0/words/search"?q=amazi" disable requiredHttpSession
  val route =
    extractMaterializer { implicit mat ⇒
      extractLog { implicit log ⇒
        pathPrefix(apiPrefix) {
          get {
            path(Segment / shared.Routes.search) { seq ⇒
              requiredHttpSession(mat.executionContext) { _ ⇒
                parameters('q.as[String], 'n ? 30) { (q, limit) ⇒
                  complete {
                    if (q.isEmpty)
                      HttpResponse(
                        entity = Chunked.fromData(
                          `text/plain(UTF-8)`,
                          chunks = SearchResults(immutable.Seq.empty[String]).source.map(ByteString(_))
                        )
                      )
                    else {

                      val sq: SearchQuery =
                        seq match {
                          case shared.Routes.searchWordsPath ⇒
                            SearchQuery.WordsQuery(q, limit, null) //TODO: improve that
                          case shared.Routes.searchHomophonesPath ⇒ SearchQuery.HomophonesQuery(q, limit, null)
                        }

                      val requestId = wvlet.airframe.ulid.ULID.newULID.toString
                      val p         = Promise[Option[SearchResults]]()
                      queue.offer((sq, p)) match {
                        case QueueOfferResult.Enqueued ⇒
                          log.info("[{}: {}] qSize: {}", sq.keyword, requestId, queue.size())
                          p.future.flatMap(
                            _.fold[Future[HttpResponse]](Future.failed(SearchTimeout(requestId, timeout.duration))) {
                              sr ⇒
                                Future.successful {
                                  HttpResponse(entity =
                                    Chunked.fromData(
                                      `text/plain(UTF-8)`,
                                      chunks = sr.source.map(word ⇒ ByteString(s"$word,"))
                                    )
                                  )
                                }
                            }
                          )
                        case QueueOfferResult.Dropped ⇒
                          Future.successful(HttpResponse(status = StatusCodes.ServiceUnavailable))
                        case other: QueueCompletionResult ⇒
                          Future.successful(HttpResponse(status = StatusCodes.InternalServerError))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
}
