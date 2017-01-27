package linguistic

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import linguistic.Panels._
import linguistic.Search._
import linguistic.SignIn._
import linguistic.SignUpModule._
import linguistic.gateaway.{SignInMode, SignUpMode, UiSession}
import org.scalajs.dom

import scala.util.control.NonFatal

object ReactJs {
  val loginSelector = "#login"
  val passwordSelector = "#password"
  val photoSelector = "#photo"

  class AppSessionBackend(scope: BackendScope[Map[String, String], UiSession]) {

    def signOut(e: ReactEventI): CallbackTo[Unit] = {
      e.preventDefaultCB >> scope.modState(s => s.copy(user = None, token = None))
    }

    def signUp(e: ReactEventI): CallbackTo[Unit] = {
      e.preventDefaultCB >> scope.modState(s => s.copy(mode = SignUpMode))
    }

    def signIn(e: ReactEventI): CallbackTo[Unit] = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
      e.preventDefaultCB >>
        CallbackTo {
          val login = dom.document.querySelector(loginSelector).asInstanceOf[dom.html.Input].value
          val password = dom.document.querySelector(passwordSelector).asInstanceOf[dom.html.Input].value
          val (headerName, headerValue) = linguistic.gateaway.signInHeader(login, password)
          linguistic.gateaway.signInAjax[shared.protocol.SignInResponse](shared.Routes.clientSignIn, Map((headerName, headerValue)))
            .map(r => scope.setState(UiSession(user = Option(r._1), token = Option(r._2))).runNow)
            .recover {
              case e: org.scalajs.dom.ext.AjaxException =>
                scope.modState(s => s.copy(error = Option("Sign in error. Check your password"))).runNow()
              case NonFatal(e)  =>
                scope.modState(s => s.copy(error = Option(s"Unexpected sign in error"))).runNow()
            }
        }
    }

    def render(session: UiSession, oauthProviders: Map[String, String], b: AppSessionBackend): ReactElement = {
      session match {
        case UiSession(None, _, SignUpMode, _) =>
          <.div(signUpComponent(session, b, oauthProviders)())

        case UiSession(None, _, SignInMode, error) =>
          <.div(
            topPanelComponent(session, oauthProviders, signUp, signOut)(),
            SignInFormArea(signIn),
            ErrorSignInFormArea(error)
          )

        case UiSession(Some(login), _, SignInMode, _) =>
          <.div(searchComponent(oauthProviders, signUp, signOut)(session))
      }
    }
  }

  def apply(domElement: dom.Element) = {
    val signInComponent = ReactComponentB[Map[String, String]]("ReactJsAppComponent")
      .initialState(UiSession(user = None, token = None))
      .backend(new AppSessionBackend(_))
      .renderPS { (scope, props, state) =>
        scope.backend.render(state, props, scope.backend)
      }.build

    //settings
    val providers = shared.HttpSettings.oauthProviders
    ReactDOM.render(signInComponent(providers), domElement)
  }
}