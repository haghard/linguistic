package linguistic.js

import scalatags.Text.all._

object AppScript {
  val divName = "content"

  def apply() =
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/main.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/chat.css"),
        script(`type` := "text/javascript", src := "/assets/lib/jquery/jquery.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/bootstrap.js"),
        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js")
      ),
      body(div(id := divName, style := "position:relative"), script(s"""linguistic.JsApplication().main('$divName')"""))
    )
}
