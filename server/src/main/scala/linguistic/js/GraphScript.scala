package linguistic.js

import scalatags.Text.all._
import scalatags.Text.svgTags.svg

object GraphScript {
  val where = "scene"

  def apply() = {
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/nv.d3.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/tree.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/graph.css")
      ),
      body(
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/d3.v3.min.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/nv.d3.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/graph.js"),

        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.min.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js"),
        script(`type` := "text/javascript", src := "/assets/ui-launcher.js"),

        svg(id := "graph"),
        script("linguistic.tree.ClusterGraphModule().main()")
      )
    )
  }
}