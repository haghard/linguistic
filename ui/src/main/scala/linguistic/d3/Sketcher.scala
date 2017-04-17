/*

package linguistic.d3

import org.scalajs.dom.html
import raphaeljs._
import scala.scalajs._
import scala.scalajs.js.annotation.JSExport

@JSExport
object Sketcher {

  def draw(scene: html.Element): Unit =  {
    val paper = Raphael(scene, 500, 500, {})
        val elems = paper.add(js.Array(
          js.Dynamic.literal(
            `type` = "circle",
            cx = 10, cy = 10, r = 5),

          js.Dynamic.literal(
            `type` = "rect",
            x = 10, y = 10,
            width = 10, height = 10,
            fill = "#fc0"
          )
        ))

        paper.forEach { el: Element =>
          el.attr(js.Dynamic.literal(stroke = "blue"))
          ()
        }

        val rect = elems(1)
        rect.drag(
          onmove = { (el: Element, dx: Double, dy: Double) =>
            el.transform(s"...t$dx $dy")
            ()
          },
          onstart = { (el: Element) =>
            el.ox = el.matrix.x(
              el.attr("x").asInstanceOf[Double],
              el.attr("y").asInstanceOf[Double]
            )
            el.oy = el.matrix.y(
              el.attr("x").asInstanceOf[Double],
              el.attr("y").asInstanceOf[Double]
            )
            el.transform(s"t${el.ox} ${el.oy}s2 2")
            ()
          },
          onend = { (el: Element) =>
            el.transform("...s1 1")
            ()
          }
        )
      }


  type EventHandler = js.Function1[org.scalajs.dom.Event,Unit]

  def action[A](f: org.scalajs.dom.Event => Unit): EventHandler = f

  @JSExport def main(scene: html.Element): Unit = {
    draw(scene)
  }
}*/
