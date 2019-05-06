package linguistic

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import linguistic.ReactJsApp.AppSessionBackend
import linguistic.gateaway.{SignInMode, UiSession}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExport
import scala.util.control.NonFatal

@JSExport
object SignUpModule {
  val loginSelector    = "#login"
  val passwordSelector = "#password"
  val photoSelector    = "#photo"

  case class SignUpSession(token: Option[String] = None, error: Option[String] = None)

  class SignUpBackend(scope: BackendScope[Unit, SignUpSession]) {
    def signUp(e: ReactEventI): CallbackTo[Unit] = {
      val login    = dom.document.querySelector(loginSelector).asInstanceOf[dom.html.Input].value
      val password = dom.document.querySelector(passwordSelector).asInstanceOf[dom.html.Input].value
      val photo    = dom.document.querySelector(photoSelector).asInstanceOf[dom.html.Input].value
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
      e.preventDefaultCB >>
      Callback {
        val (h, v) = linguistic.gateaway.signUpHeader(login, password, photo)
        linguistic.gateaway
          .signUp(shared.Routes.clientSignUp, Map((h, v)))
          .map { response =>
            val token = response.getResponseHeader(shared.Headers.fromServer)
            scope.modState(_.copy(token = Option(token))).runNow()
          }
          .recover {
            case e: org.scalajs.dom.ext.AjaxException =>
              //org.scalajs.dom.console.log(msg)
              val msg = s"Error: Login $login isn't unique !!! Try another one."
              scope.modState(_.copy(error = Option(msg))).runNow()
            case NonFatal(e) =>
              val msg = s"Unexpected error at sign up stage"
              scope.modState(_.copy(error = Option(msg))).runNow()
          }
      }
      //Callback.log(s"$login $password $photo")  >> CallbackTo { scope.modState(s =>s.copy(login=Option(login))).runNow() }
    }

    val SignUpFormArea = ReactComponentB[Unit]("SignInForm").stateless.render { _ =>
      <.div(
        ^.cls := "container-fluid",
        <.form(
          ^.cls := "signin",
          ^.role := "form",
          <.div(
            "New user",
            ^.fontSize := "16px",
            ^.color.black
            //, ^.cls := "container-fluid"
          ),
          <.input(
            ^.id := "login",
            ^.cls := "form-control",
            ^.`type` := "text",
            ^.placeholder := "Login"
          ),
          <.input(
            ^.id := "photo",
            ^.cls := "form-control",
            ^.`type` := "text",
            ^.placeholder := "Proto url"
          ),
          <.input(
            ^.id := "password",
            ^.cls := "form-control",
            ^.`type` := "password",
            ^.placeholder := "Password"
          ),
          <.button(
            "sign up",
            ^.cls := "btn btn-lg btn-primary btn-block",
            ^.onClick ==> signUp
          )
        )
      )
    }.build

    val ErrorSignUpFormArea = ReactComponentB[String]("SignInErrorComp").stateless.render_P { errorMessage =>
      val ariaHidden = "aria-hidden".reactAttr
      <.div(
        errorMessage,
        ^.id := "sign-error",
        ^.cls := "alert alert-danger role=alert",
        <.span(^.cls := "glyphicon glyphicon-exclamation-sign", ariaHidden := "true"),
        <.span("Error:", ^.cls := "sr-only")
      )
    }.build

    def render(
      state: SignUpSession,
      back: AppSessionBackend,
      oauthProviders: Map[String, String],
      session: UiSession
    ): ReactElement =
      state match {
        case SignUpSession(None, None) =>
          <.div(SignUpFormArea())
        case SignUpSession(None, Some(error)) =>
          <.div(
            SignUpFormArea(),
            ErrorSignUpFormArea(error)
          )
        case SignUpSession(Some(_), _) =>
          back.render(session.copy(mode = SignInMode, error = None), oauthProviders, back)
      }

    def render2(state: SignUpSession): ReactElement =
      state match {
        case SignUpSession(None, None) =>
          <.div(SignUpFormArea())
        case SignUpSession(None, Some(error)) =>
          <.div(SignUpFormArea(), ErrorSignUpFormArea(error))
      }
  }

  def signUpComponent(s: UiSession, b: AppSessionBackend, oauthProviders: Map[String, String]) =
    ReactComponentB[Unit]("SignUpComponent")
      .initialState(SignUpSession())
      .backend(new SignUpBackend(_))
      .renderPS { (scope, props, state) =>
        scope.backend.render(state, b, oauthProviders, s)
      }
      .build

  //https://192.168.0.62:9443/signup
  def apply() = {
    val content = dom.document.getElementById("content")
    val signUpComp = ReactComponentB[Unit]("SignUpComponent2")
      .initialState(SignUpSession())
      .backend(new SignUpBackend(_))
      .renderPS { (scope, props, state) =>
        scope.backend.render2(state)
      }
      .build
    //import chandu0101.scalajs.react.components.Implicits._
    ReactDOM.render(signUpComp(), content)
  }

  @JSExport
  def main() =
    apply()
}
