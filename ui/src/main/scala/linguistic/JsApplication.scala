package linguistic

import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport

@JSExport
object JsApplication {

  @JSExport
  def main(where: String): Unit = {
    val content = dom.document.getElementById(where)
    ReactJsApp(content)
  }
}