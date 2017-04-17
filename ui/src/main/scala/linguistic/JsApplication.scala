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
  }
}