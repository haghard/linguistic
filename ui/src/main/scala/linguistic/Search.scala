package linguistic

import japgolly.scalajs.react.Addons.ReactCssTransitionGroup
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.ext.Ajax

import scala.util.{Failure, Success}

object Search {

  val alignContent = "align-content".reactAttr

  case class SearchWordsState(query: String = "", limit: Int = 10,
                              words: Seq[String] = Seq.empty[String])

  class SearchWordsBackend(scope: BackendScope[_, SearchWordsState]) {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    def onChange(searchType: String, token: String)(e: ReactEventI) = {
      //Callback.log(e.target.value) >>
      Callback {
        val q = e.target.value
        Ajax.get(shared.Routes.search(searchType, q, 50),
          //s"${shared.Routes.pref}/${shared.Routes.v1}/${searchType}/${shared.Routes.search(q, 50)}",
          headers = Map((shared.Headers.fromClient -> token))).onComplete {
          case Success(r) =>
            val resp = r.responseText.split(",")
            //dom.console.log("Http OK", resp.size)
            scope.modState { s => s.copy(query = q, words = resp) }.runNow()
          case Failure(ex) =>
            //dom.console.log("Http Error: " + ex.getMessage)
            scope.modState { s => s.copy(query = q) }.runNow()
        }
      }
    }
  }

  val SearchBoxesComponent = ReactComponentB[(SearchWordsState, SearchWordsBackend, String)]("SearchBoxComp")
    .stateless
    .render_P { case (state, backend, token) =>
      <.div(
        <.form(^.cls := "heading-container", alignContent := "center", ^.role := "form",
          <.div(^.cls := "row",
            <.div(
              ^.cls := "form-group col-sm-7 col-md-8",
              <.div(
                ^.cls := "input-group",
                <.span(
                  ^.id := "search-addon",
                  ^.cls := "input-group-addon",
                  <.div(
                    "Words list",
                    ^.fontSize := "9px",
                    ^.color.black
                    //^.cls := "container-fluid"
                  ),
                  <.span(
                    ^.cls := "glyphicon glyphicon-search"
                  )
                ),
                <.div(
                  <.input(
                    ^.id := "search-by-pref",
                    ^.`type` := "text",
                    ^.cls := "form-control",
                    ^.onChange ==> backend.onChange(shared.Routes.searchWordsPath, token)
                  )
                )
              )
            ),
            <.div(^.cls := "col-sm-5 col-md-4",
              <.div(^.cls := "row",
                <.div(^.cls := "col-sm-9 col-xs-9",
                  <.div(^.cls := "form-group",
                    <.div(
                      ^.cls := "input-group",
                      <.span(
                        ^.id := "location-addon",
                        ^.cls := "input-group-addon",
                        <.div(
                          "Homophones",
                          ^.fontSize := "9px",
                          ^.color.black
                          //^.cls := "container-fluid"
                        ),
                        <.span(
                          ^.cls := "glyphicon glyphicon-search"
                        )
                        //glyphicon-map-marker
                      ),
                      <.div(
                        <.input(
                          ^.id := "search-by-location",
                          ^.`type` := "text",
                          ^.cls := "form-control",
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

  //<.div(^.id := "outs", ^.cls := "container-fluid", <.div(^.id := "outA"), <.div(^.id := "outB"))

  class SearchOutBackend($: BackendScope[_, SearchWordsState]) {
    def render(state: SearchWordsState) = {
      <.div(
        ^.id := "outs",
        ^.fontSize := "10px", ^.color.black,
        ^.cls := "container-fluid",
        <.div(
          ReactCssTransitionGroup("search-output", component = "h5") {
            state.words.map { word =>
              <.div(^.key := word, word)
            }
            /*
            state.out.zipWithIndex.map { case (word, i) =>
                <.div(
                  ^.key := word,
                  //^.onClick --> handleRemove(i),
                  word
                )
              }: _*
            */
          }
        )
      )
    }
  }

  def searchResultsComponent(state: SearchWordsState) = {
    ReactComponentB[Unit]("SearchResultsComponent")
      .initialState(state)
      .renderBackend[SearchOutBackend]
      .build
  }
}