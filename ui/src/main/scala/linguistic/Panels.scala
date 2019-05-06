package linguistic

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactEventI, _}
import linguistic.gateaway.UiSession
import shared.protocol.SignInResponse

object Panels {

  def topPanelLeftArea(signUp: (ReactEventI => CallbackTo[Unit]), signOut: (ReactEventI => CallbackTo[Unit])) =
    ReactComponentB[Option[SignInResponse]]("LeftSidePanel").stateless.render_P { props =>
      props.fold(
        <.div(
          ^.cls := "navbar-header",
          <.a(
            "Sign up",
            ^.cls := "navbar-brand",
            ^.onClick ==> signUp
            //^.href := "/signup"
          )
        )
      ) { _ =>
        <.div(^.cls := "navbar-header", <.a("Sign out", ^.cls := "navbar-brand", ^.href := "/"))
      }
    }.build

  def topPanelRightArea(providers: Map[String, String]) =
    ReactComponentB[Option[SignInResponse]]("RightSidePanel").stateless.render_P { props =>
      props.fold(
        <.div(
          ^.id := "auth-providers",
          ^.cls := "navbar-collapse",
          <.ul(
            ^.cls := "nav navbar-nav navbar-right",
            for (kv <- providers) yield {
              <.li(<.a(kv._1, ^.href := kv._2))
            }
          )
        )
      ) { auth =>
        <.div(
          ^.id := "user-info",
          ^.cls := "navbar-collapse",
          <.ul(
            ^.cls := "nav navbar-nav navbar-right",
            <.li(<.a(s"${auth.login} last login: 21-01-16 00:01:12" /*, ^.href := "#"*/ )),
            <.li(<.img(^.src := auth.photo, ^.cls := "account-image img-circle"))
          )
        )
      }
    }.build

  def topPanelComponent(
    s: UiSession,
    oauthProviders: Map[String, String],
    signUp: (ReactEventI => CallbackTo[Unit]),
    signOut: (ReactEventI => CallbackTo[Unit])
  ) =
    ReactComponentB[Unit]("TopPanel").stateless.render { _ =>
      <.nav(
        ^.cls := "navbar navbar-default",
        <.div(
          ^.cls := "container-fluid",
          topPanelLeftArea(signUp, signOut)(s.user),
          topPanelRightArea(oauthProviders)(s.user)
        )
      )
    }.build
}
