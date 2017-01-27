package linguistic.js

import scalatags.Text.all._

object AnimalsScript {

  def apply() = {
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/nv.d3.css")
      ),
      body(
        script(`type` := "text/javascript", src := "/assets/lib/jquery/jquery.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/bootstrap.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/colorbrewer.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/d3.v3.min.js"),

        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.min.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js"),
        script(`type` := "text/javascript", src := "/assets/ui-launcher.js"),

        div(id := "content", cls := "center"),

        script("linguistic.tree.AnimalsModule().main()")
        
        //script("linguistic.d3.TweetsModule().main()")
      )
    )
  }
}
