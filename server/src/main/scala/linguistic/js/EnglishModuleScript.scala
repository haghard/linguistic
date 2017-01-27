package linguistic.js

import scalatags.Text.all._

object EnglishModuleScript {
  val where = "content"
  //val where2 = "scene"

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
            h1(id := "logo", "Word Tree" /*a(href:=".", "Word Tree")*/),

            form(id := "form-source", style :="display: none;",
              input(`type` :=  "text", id := "source", placeholder :=  "")),

            form(id := "form", style := "display: inline-block;",
              input(`type` := "text", id := "keyword", placeholder := "Keyword…"),
              span("  "),
              label(`for` := "reverse", style := "font-size: small", input(id := "reverse", `type` := "checkbox", "reverse tree"))
              //label(`for` := "phrase-line", style := "font-size: small", input(id :="phrase-line", `type` := "checkbox", "one phrase per line"))
            )
          )
        ),

        div(id := where, cls := "center"),
        //div(id := where2, cls := "center"),

        script(s"linguistic.tree.EnglishModule().main('${where}')")

        /*
        script(s"""
          window.onload = function() {
              // Get a reference to the canvas object
              var scene = document.getElementById('$where2');
              // Call the scalajs app entry point
              linguistic.d3.Sketcher().main(scene);
          }
        """)*/
      )
    )
  }
}