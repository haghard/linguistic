package linguistic.other

import japgolly.scalajs.react.Addons.ReactCssTransitionGroup
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import org.scalajs.dom.window

object ToDo2 {

  class TodoListBackend($: BackendScope[Unit, Vector[String]]) {

    def handleAdd =
      $.modState {
        _ :+ window.prompt("Enter a word: ")
      }

    def handleRemove(i: Int) = {
      $.modState(_.zipWithIndex.filterNot(_._2 == i).map(_._1))
    }

    def render(state: Vector[String]) = {
      dom.console.log("todo-list-backend hab been re-rendered")
      <.div(
        ^.color.black,
        //^.fontSize := "12px",
        ^.cls := "container-fluid",
        <.div(
          <.button(^.onClick --> handleAdd, "Add word"),
          ReactCssTransitionGroup("example", component = "h5")(
            state.zipWithIndex.map { case (s, i) =>
              dom.console.log("add item in todo-list-backend")
              <.div(
                ^.key := s,
                ^.onClick --> handleRemove(i),
                s
              )
            }: _*
          )
        )
      )
    }
  }

  val TodoList = ReactComponentB[Unit]("TodoList")
    .initialState(Vector("hello", "world"))
    .renderBackend[TodoListBackend]
    .build
}