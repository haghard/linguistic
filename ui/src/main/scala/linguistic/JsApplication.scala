package linguistic

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

object JsApplication extends js.JSApp {

  @JSExport
  override def main(): Unit = {
    val content = dom.document.getElementById("content")
    content.removeChild(content.firstChild)
    ReactJs(content)

    /*
      import scalatags.JsDom.all._

      object SignIn {
        val login = "#login"
        val password = "#password"
      }

      def signIn(form: dom.Element, event: dom.Event) = {
        event.preventDefault
        val login = dom.document.querySelector(SignIn.login).asInstanceOf[dom.html.Input].value
        val password = dom.document.querySelector(SignIn.password).asInstanceOf[dom.html.Input].value

        val onSuccess = (data: js.Any, headers: jquery.JQueryXHR) => {
          dom.document.write(data.toString)
        }

        val onFailure = (resp: jquery.JQueryXHR) => {
          val errorMessage =
            div(
              "Enter a valid login and password",
              id := "sign-error",
              cls := "alert alert-danger role=alert",
              span(
                cls :=  "glyphicon glyphicon-exclamation-sign",
                attr("aria-hidden") := "true"
              ),
              span(
                "Error:",
                cls := "sr-only"
              )
            ).render

          form.appendChild(errorMessage)
          dom.window.setTimeout({ () => form.removeChild(errorMessage) }, 5000l)
        }

        val settings: jquery.JQueryAjaxSettings =
          js.Dynamic.literal(
            timeout = 1000d,
            //headers = reqHeaders,
            method = "GET",
            url = s"""authenticate?login=${login}&password=${password}""", //at least BASE64
            //data = payload,
            success = (data: js.Any, jqXHR: jquery.JQueryXHR) =>
              //onSuccess(JSON.parse(data.asInstanceOf[String]).asInstanceOf[js.Dictionary[js.Any]], jqXHR),
              onSuccess(data, jqXHR),
            error = (jqXHR: jquery.JQueryXHR) =>
              onFailure(jqXHR)
          ).asInstanceOf[jquery.JQueryAjaxSettings]

        jquery.jQuery.ajax(settings)
      }
      */


    /*
    import org.singlespaced.d3js.d3
    import org.singlespaced.d3js.Ops._

    import scalatags.JsDom.all._
    dom.document.body.appendChild(link(rel := "stylesheet", href := "/assets/lib/bootstrap/css/main.css").render)

    val signin = dom.document.getElementById("signin-form")
    if (signin ne null) {
      signin.removeChild(signin.firstChild)
      signin.appendChild(
        div(
          cls := "container-fluid",
          form(
            cls := "form-signin",
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

            button(
              "Sing in",
              cls := "btn btn-lg btn-primary btn-block",
              onclick := action(signIn(signin, _))
            )
          )
        ).render
      )
    } else {
      val searchA = dom.document.getElementById("searchA")

      import org.singlespaced.d3js.d3
      import org.singlespaced.d3js.Ops._

      val searchOutputA = dom.document.getElementById("outA")

      searchA.removeChild(searchA.firstChild)

      val searchInputA = input(
        id := "search-by-pref",
        `type` := "text",
        spellcheck := "false",
        cls := "form-control"
      ).render

      //binding

      searchInputA.onkeyup = action { event: dom.Event =>
        searchOutputA.textContent = searchInputA.value
      }

      searchA.appendChild(searchInputA)

      val searchB = dom.document.getElementById("searchB")
      searchB.removeChild(searchB.firstChild)
      searchB.appendChild(
        input(
          id := "search-by-location",
          `type` := "text",
          spellcheck := "false",
          cls := "form-control",
          attr("data-reactid") := ".0.3.0.0.0.0.0.0.1",
          attr("aria-describedby") := "search-addon"
          //oninput := action(search("b")(_))
        ).render
      )
    }*/
  }
}