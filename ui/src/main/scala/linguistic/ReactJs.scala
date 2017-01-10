package linguistic

import linguistic.Panels._
import linguistic.Search._
import linguistic.Sign._
import linguistic.ui.UiSession
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom

object ReactJs {

  class AppSessionBackend(scope: BackendScope[Map[String, String], UiSession]) {
    val loginS = "#login"
    val passwordS = "#password"

    def signIn(e: ReactEventI): CallbackTo[Unit] = {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
      e.preventDefaultCB >>
        CallbackTo {
          val login = dom.document.querySelector(loginS).asInstanceOf[dom.html.Input].value
          val password = dom.document.querySelector(passwordS).asInstanceOf[dom.html.Input].value

          val (h, v) = linguistic.ui.signInHeader(login, password)
          linguistic.ui.httpSignIp[shared.protocol.SignInResponse](shared.Routes.clientSignIn, Map((h, v))).onSuccess {
            case Right(r) => scope.setState(UiSession(Option(r._1), token = Option(r._2))).runNow()
            case Left(ex) => scope.modState(s => s.copy(error = Option("Error: " + ex))).runNow()
          }
        }
    }

    //
    def render(session: UiSession, oauthProviders: Map[String, String]) = {
      session.entry match {
        case None =>
          <.div(
            topPanelComponent(session, oauthProviders)(),
            SignFormArea(signIn),
            ErrorSignInFormArea(session.error)
          )

        case Some(auth) =>
          val SearchComponent = ReactComponentB[UiSession]("SearchComponent")
            .initialState(SearchWordsState())
            .backend(new SearchWordsBackend(_))
            .renderPS { ($, session, searchState) =>
              <.div(
                ^.cls := "container-fluid",
                <.div(
                  topPanelComponent(session, oauthProviders)(),
                  SearchBoxesComponent((searchState, $.backend, session.token.get)),
                  searchResultsComponent(searchState)()
                )
              )
            }.build

          <.div(SearchComponent(session))
      }
    }
  }

  def apply(domElement: dom.Element) = {
    val app = ReactComponentB[Map[String, String]]("ReactJsAppComponent")
      .initialState(UiSession(None, None))
      //.renderBackend[ApplicationSessionBackend]
      .backend(new AppSessionBackend(_))
      .renderPS { (scope, props, state) =>
        scope.backend.render(state, props)
      }.build

    val props = shared.HttpSettings.oauthProviders
    ReactDOM.render(app(props), domElement)
  }
}