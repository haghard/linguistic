package linguistic
/*
import akka.actor.ActorSystem
//import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling._
import akka.stream.Materializer
//import geolocation.TimelineRoutes.ChatterJsonSupport
import geolocation.domain.TimelineAccess
import geolocation.domain.TimelineAccess
import shared.Routes
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object TimelineRoutes {

  case class SaveMessageCmd(author: String, message: String)

  case class ReadTimeLineCmd(name: String, fromTimeUuid: String)
/*
  trait ChatterJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val printer = CompactPrinter
    //implicit val itemFormat = jsonFormat2(SaveMessageCmd)

    import scala.concurrent.duration._

    implicit val saveUnmarshaller = new FromRequestUnmarshaller[SaveMessageCmd]() {
      override def apply(value: HttpRequest)(implicit ec: ExecutionContext, materializer: Materializer): Future[SaveMessageCmd] = {
        value.entity.toStrict(5 seconds).map(_.data.utf8String)
          .map { paramsLine =>
            //name=legorby2e&message=Hello
            val parts = paramsLine.split("&")
            SaveMessageCmd(parts(0).split("=")(1), parts(1).split("=")(1))
          }
      }
    }
  }*/
}

class TimelineRoutes(dao: TimelineAccess)(implicit val system: ActorSystem) extends Directives with AuthTokenSupport with ChatterJsonSupport {

  import TimelineRoutes._

  import scala.concurrent.duration._

  private def respondWithError(error: String) =
    complete(HttpResponse(InternalServerError, entity = JsObject(("errors", JsObject("messages" -> JsArray(JsString(error))))).compactPrint))

  val timelineTimeout = 2 seconds

  val route: Route = withRequestTimeout(timelineTimeout) {
    extractMaterializer { implicit mat =>
      extractExecutionContext { implicit ec =>
        extractLog { log =>
          (post & path(Routes.timeline)) {
            requiredHttpSession(ec) { session ⇒
              entity(as[SaveMessageCmd]) { saveCmd =>
                onSuccess(dao.saveMessage(saveCmd)) {
                  case Right(r) => complete(s""" { "result": $r } """)
                  case Left(ex) => respondWithError(ex)
                }
              }
            }
          } ~
            (get & path(Routes.timeline)) {
              requiredHttpSession(ec) { session ⇒
                parameters('name.as[String], 'timeuuid.as[String]) {
                  (timeline, fromTimeuuid) =>
                  onSuccess(dao.timeline(timeline, fromTimeuuid, session.login)) {
                    case Right(messages) =>
                      complete {
                        JsObject(
                          ("login" -> JsString(session.login)),
                          ("messages" -> JsArray(
                            messages.map { m =>
                              JsObject(
                                "author" -> JsString(m.author),
                                "message" -> JsString(m.message),
                                "timeuuid" -> JsString(m.timeuuid),
                                "time" -> JsString(m.time))
                            }
                          ))).compactPrint
                      }
                    case Left(ex) => respondWithError(s"Couldn't load messages for ${session.login} ")
                  }
                }
              }
            } ~ (get & path(Routes.timeline)) {
            requiredHttpSession(ec) { session ⇒
              parameters('name.as[String], 'latest.as[Int]) {
                (timeline, depth) =>
                onSuccess(dao.timelineLatest(timeline, depth, session.login)) {
                  case Right(messages) =>
                    complete {
                      val chatLog = JsObject(
                        ("login" -> JsString(session.login)),
                        ("messages" -> JsArray(
                          messages.map { m =>
                            JsObject("author" -> JsString(m.author), "message" -> JsString(m.message),
                              "timeuuid" -> JsString(m.timeuuid),
                              "time" -> JsString(m.time))
                          }
                        )))
                      chatLog.compactPrint
                    }
                  case Left(ex) => respondWithError(s"Couldn't load messages for ${session.login} ")
                }
              }
            }
          }
        }
      }
    }
  }
}*/
