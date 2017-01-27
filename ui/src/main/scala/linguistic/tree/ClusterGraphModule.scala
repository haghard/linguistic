package linguistic.tree

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object ClusterGraphModule {

  val data = js.Dictionary(
    "nodes" -> js.Array(
      js.Dynamic.literal("name" -> "192.168.0.1", "group" -> "inventory"),
      js.Dynamic.literal("name" -> "192.168.0.2", "group" -> "inventory"),
      js.Dynamic.literal("name" -> "192.168.0.3", "group" -> "inventory"),
      js.Dynamic.literal("name" -> "192.168.0.4", "group" -> "inventory")
    ),
    "links" -> js.Array(
      js.Dynamic.literal("source" -> 0, "target" -> 1, "value" -> 1),
      js.Dynamic.literal("source" -> 1, "target" -> 2, "value" -> 2),
      js.Dynamic.literal("source" -> 2, "target" -> 3, "value" -> 3),
      js.Dynamic.literal("source" -> 3, "target" -> 0, "value" -> 4)
    )
  )

  @JSExport
  def main(): Unit = {
    js.Dynamic.global.drawGraph(data)
  }
}