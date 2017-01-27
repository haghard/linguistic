package linguistic.js

import scalatags.Text.all._

object ObamaSpeechScript {
  val scene = "content"

  def apply() = {
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/nv.d3.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/tree.css")
      ),
      body(
        script(`type` := "text/javascript", src := "/assets/lib/jquery/jquery.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/bootstrap.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/raphael.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/word-tree-layout.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/wordtree.js"),

        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.min.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js"),
        script(`type` := "text/javascript", src := "/assets/ui-launcher.js"),

        div(id :="header",
          div(cls := "wrapper",
            h1(id := "logo", "Search" /*a(href:=".", "Word Tree")*/),

            form(id := "form-source", style :="display: none;",
              input(`type` :=  "text", id := "source", placeholder :=  "")),

            form(id := "form", style := "display: inline-block;",
              input(`type` := "text", id := "keyword", placeholder := "…"))
          )
        ),

        div(id := scene, cls := "center"),
        script(s"linguistic.tree.ObamaSpeechModule().main('${scene}')")
      )
    )
  }
}