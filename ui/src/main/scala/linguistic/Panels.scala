package linguistic

import linguistic.ui.UiSession
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import shared.protocol.SignInResponse

object Panels {

  private[this] def signUp(e: ReactEventI): CallbackTo[Unit] = {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
    e.preventDefaultCB >>
    Callback {
      val (h,v) = linguistic.ui.signUpHeader("haghard84@gmail.com", "suBai3sa", "https://avatars.githubusercontent.com/u/1887034?v=3")
      linguistic.ui.httpSignUp(shared.Routes.clientSignUp, Map((h, v))).onSuccess {
        case Right(token) => Callback.log("sign-up success")
        case Left(ex) => Callback.log(s"sign-up error: $ex")
      }
    }
  }

  val TopPanelLeftArea = ReactComponentB[Option[SignInResponse]]("LeftSidePanel")
    .stateless
    .render_P { props =>
      props.fold(
        <.div(
          ^.cls := "navbar-header",
          <.a("Sign up", ^.cls := "navbar-brand", ^.onClick ==> signUp
             /*href := "#"*/)
        )
      ) { _ =>
        <.div(^.cls := "navbar-header",
          <.a("Sign out", ^.cls := "navbar-brand", ^.href := "#")
        )
      }
    }.build

  def topPanelRightArea(providers: Map[String, String]) =
    ReactComponentB[Option[SignInResponse]]("RightSidePanel")
      .stateless
      .render_P { props =>
        props.fold(
          <.div(
            ^.id := "auth-providers", ^.cls := "navbar-collapse",
            <.ul(
              ^.cls := "nav navbar-nav navbar-right",
              for (kv <- providers) yield {
                <.li(<.a(kv._1, ^.href := kv._2))
              }
            )
          )
        ) { auth =>
          <.div(
            ^.id := "user-info", ^.cls := "navbar-collapse", //collapse
            <.ul(
              ^.cls := "nav navbar-nav navbar-right",
              <.li(<.a(auth.login, ^.href := "#")),
              <.li(<.img(^.src := auth.photo, ^.cls := "account-image img-circle"))
            )
          )
        }
      }.build

  def topPanelComponent(s: UiSession, oauthProviders: Map[String, String]) = ReactComponentB[Unit]("TopPanel")
    .stateless
    .render { _ =>
      <.nav(
        ^.cls := "navbar navbar-default",
        <.div(
          ^.cls := "container-fluid",
          TopPanelLeftArea(s.entry),
          topPanelRightArea(oauthProviders)(s.entry)
        )
      )
    }.build
}
