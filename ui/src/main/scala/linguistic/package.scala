import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.control.NonFatal

/*

package object geolocation {

  type EventHandler = js.Function1[dom.Event, Unit]

  def action[A](f: dom.Event => Unit): EventHandler = f

  def handler(pref: String) = action { (event: dom.Event) =>
    dom.console.log(pref + " " + event.target.asInstanceOf[dom.raw.HTMLInputElement].value)
  }


}
*/
