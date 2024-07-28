package linguistic

import japgolly.scalajs.react.Addons.ReactCssTransitionGroup
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import linguistic.Panels.topPanelComponent
import linguistic.gateaway.UiSession
import org.scalajs.dom.ext.Ajax

import scala.util.{Failure, Success}

object Search {

  val alignContent = "align-content".reactAttr

  case class SearchWordsState(query: String = "", limit: Int = 20, words: Seq[String] = Seq.empty[String])

  class SearchWordsBackend(scope: BackendScope[_, SearchWordsState]) {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    def onChange(searchType: String, token: String)(e: ReactEventI) =
      Callback {
        // Callback(e.target.value = "")

        val q = e.target.value
        Ajax
          .get(
            shared.Routes.search(searchType, q, 30),
            headers = Map(shared.Headers.fromClient -> token)
          )
          .onComplete {
            case Success(r) =>
              val wordsResp = r.responseText.split(",")
              scope.modState(_.copy(query = q, words = wordsResp)).runNow()
            case Failure(ex) =>
              scope
                .modState { s =>
                  s.copy(query = q)
                }
                .runNow()
          }
      }
  }

  val SearchComponent =
    ReactComponentB[(SearchWordsState, SearchWordsBackend, String)]("SearchBoxComp").stateless.render_P {
      case (state, backend, token) =>
        <.div(
          <.form(
            ^.cls        := "heading-container",
            alignContent := "center",
            ^.role       := "form",
            <.div(
              ^.cls := "row",
              <.div(
                ^.cls := "form-group col-sm-7 col-md-8",
                <.div(
                  ^.cls := "input-group",
                  <.span(
                    ^.id  := "search-addon",
                    ^.cls := "input-group-addon",
                    <.div(
                      "Words list",
                      ^.fontSize := "9px",
                      ^.color.black
                      // ^.cls := "container-fluid"
                    ),
                    <.span(^.cls := "glyphicon glyphicon-search")
                  ),
                  <.div(
                    <.input(
                      ^.id     := "search-by-pref",
                      ^.`type` := "text",
                      ^.cls    := "form-control",
                      ^.onChange ==> backend.onChange(shared.Routes.searchWordsPath, token)
                    )
                  )
                )
              ),
              <.div(
                ^.cls := "col-sm-5 col-md-4",
                <.div(
                  ^.cls := "row",
                  <.div(
                    ^.cls := "col-sm-9 col-xs-9",
                    <.div(
                      ^.cls := "form-group",
                      <.div(
                        ^.cls := "input-group",
                        <.span(
                          ^.id  := "location-addon",
                          ^.cls := "input-group-addon",
                          <.div(
                            "Homophones",
                            ^.fontSize := "9px",
                            ^.color.black
                            // ^.cls := "container-fluid"
                          ),
                          <.span(
                            ^.cls := "glyphicon glyphicon-search"
                          )
                          // glyphicon-map-marker
                        ),
                        <.div(
                          <.input(
                            ^.id     := "search-by-location",
                            ^.`type` := "text",
                            ^.cls    := "form-control",
                            ^.onChange ==> backend.onChange(shared.Routes.searchHomophonesPath, token)
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
    }.build

  // <.div(^.id := "outs", ^.cls := "container-fluid", <.div(^.id := "outA"), <.div(^.id := "outB"))

  class SearchOutBackend($ : BackendScope[_, SearchWordsState]) {
    def render(state: SearchWordsState) =
      <.div(
        ^.id       := "outs",
        ^.fontSize := "10px",
        ^.color.black,
        ^.cls := "container-fluid",
        <.div(
          ReactCssTransitionGroup("search-output", component = "h5") {
            state.words.map(word => <.div(^.key := word, word))
          }
        )
      )
  }

  def searchComponent(
    oauthProviders: Map[String, String],
    signUp: (ReactEventI => CallbackTo[Unit]),
    signOut: (ReactEventI => CallbackTo[Unit])
  ) =
    ReactComponentB[UiSession]("SearchComponent")
      .initialState(SearchWordsState())
      .backend(new SearchWordsBackend(_))
      .renderPS { ($, session, searchState) =>
        <.div(
          ^.cls := "container-fluid",
          <.div(
            topPanelComponent(session, oauthProviders, signUp, signOut)(),
            SearchComponent((searchState, $.backend, session.token.get)),
            ReactComponentB[Unit]("SearchResults")
              .initialState(searchState)
              .renderBackend[SearchOutBackend]
              .build()
          )
        )
      }
      .build
}
