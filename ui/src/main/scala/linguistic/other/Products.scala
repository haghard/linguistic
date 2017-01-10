package linguistic.other

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object Products {

  case class SearchState(filterText: String, inStockOnly: Boolean)

  case class Product(name: String, price: Double, category: String, stocked: Boolean)

  class SearchBackend($: BackendScope[_, SearchState]) {

    def onChange(e: ReactEventI) = {
      Callback.log(e.target.value) >>
      e.extract(_.target.value) { value =>
        $.modState(_.copy(filterText = value))
      }
    }

    def onCheckBox(e: ReactEvent) =
      $.modState(s => s.copy(inStockOnly = !s.inStockOnly))
  }


  val SearchBar = ReactComponentB[(SearchState, SearchBackend)]("SearchBar")
    .render_P { case (state, backend) =>
      <.form(^.color.black,
        <.input.text(
          ^.placeholder := "Search Bar ...",
          ^.value := state.filterText,
          ^.onChange ==> backend.onChange
        ),
        <.p(
          <.input.checkbox(^.onClick ==> backend.onCheckBox), "Only show products in stock"
        )
      )
    }.build

  def productFilter(s: SearchState)(p: Product): Boolean =
    p.name.contains(s.filterText) && (!s.inStockOnly || p.stocked)

  val ProductCategoryRow = ReactComponentB[String]("ProductCategoryRow")
    .render_P { category =>
      <.tr(<.th(^.colSpan := 2, category))
    }.build

  val ProductRow = ReactComponentB[Product]("ProductRow")
    .render_P { p =>
      <.tr(
        <.td(<.span(!p.stocked ?= ^.color.red, p.name)),
        <.td(p.price))
    }.build

  val ProductTable = ReactComponentB[(List[Product], SearchState)]("ProductTable")
    .render_P { case (products, state) =>
      //org.scalajs.dom.console.log("filter: " + state.filterText)
      val rows = products.filter(productFilter(state))
        .groupBy(_.category).toList
        .flatMap { case (cat, ps) =>
          ProductCategoryRow.withKey(cat)(cat) :: ps.map(p => ProductRow.withKey(p.name)(p))
        }
      <.table(
        ^.color.black,
        <.thead(<.tr(<.th("Name"), <.th("Price"))),
        <.tbody(rows)
      )
    }.build

  val FilterableProductTable = ReactComponentB[List[Product]]("FilterableProductTable")
    .initialState(SearchState("", false))
    .backend(new SearchBackend(_))
    .renderPS { (scope, props, state) =>
      <.div(
        ^.cls := "container-fluid",
        <.div(
          SearchBar((state, scope.backend)),
          ProductTable((props, state))
        )
      )
    }.build
}
