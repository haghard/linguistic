package linguistic.other

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object ToDo {

  val TodoList = ReactComponentB[List[String]]("TodoList")
    .render_P { props =>
      def createItem(itemText: String) = <.li(itemText)
      <.ul(props map createItem)
    }.build

  case class State(items: List[String], text: String)

  class Backend($: BackendScope[Unit, State]) {
    def onChange(e: ReactEventI) = {
      val newValue = e.target.value
      $.modState(_.copy(text = newValue))
    }

    def handleSubmit(e: ReactEventI) =
      e.preventDefaultCB >>
        $.modState(s => State(s.items :+ s.text, ""))

    def render(state: State) =
      <.div(
        <.h3("TODO"),
        TodoList(state.items),
        <.form(^.onSubmit ==> handleSubmit,
          <.input(^.onChange ==> onChange, ^.value := state.text),
          <.button("Add #", state.items.length + 1)
        )
      )
  }

  val TodoApp = ReactComponentB[Unit]("TodoApp")
    .initialState(State(Nil, ""))
    .renderBackend[Backend]
    .build
}
