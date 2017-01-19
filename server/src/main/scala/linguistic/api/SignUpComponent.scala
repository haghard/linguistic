package linguistic.api

import scalatags.Text.all._

object SignUpComponent {

  def apply() = {
    html(
      head(
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/bootstrap.css"),
        link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/main.css")
      ),
      body(
        script(`type` := "text/javascript", src := "/assets/lib/jquery/jquery.js"),
        script(`type` := "text/javascript", src := "/assets/lib/bootstrap/js/bootstrap.js"),

        script(`type` := "text/javascript", src := "/assets/ui-jsdeps.min.js"),
        script(`type` := "text/javascript", src := "/assets/ui-opt.js"),
        script(`type` := "text/javascript", src := "/assets/ui-launcher.js"),

        div(id := "content", "A web form should be displayed here"),
/*
        div(
          id := where,
          cls := "center",
          div(cls := "container-fluid",
            form(
              cls := "signin",
              role := "form",
              input(
                id := "login",
                cls := "form-control",
                `type` := "text",
                placeholder := "Login"
              ),
              input(
                id := "password",
                cls := "form-control",
                `type` := "password",
                placeholder := "Password"
              ),
              input(
                id := "photo",
                cls := "form-control",
                `type` := "text",
                placeholder := "Photo url"
              ),
              button(
                "sign up",
                cls := "btn btn-lg btn-primary btn-block"
                //onclick := click _
              )
            )
          )
        )*/

        script("linguistic.SignUp().main()")
      )
    )
  }
}