package linguistic.api

import scalatags.Text.all._


object HistogramComponent {

  def apply(where: String) = {
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/nv.d3.css")
      ),
      body(
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/nv.d3.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/bootstrap.js"),

        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.min.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js"),
        script(`type` := "text/javascript", src := "/assets/ui-launcher.js"),

        div(id := where, cls := "center"),
        script(s"geolocation.d3.Histogram().main('#${where}')")
      )
    )
  }
}
