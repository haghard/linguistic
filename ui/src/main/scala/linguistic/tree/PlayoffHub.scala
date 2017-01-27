package linguistic.tree

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object PlayoffHub {
  val w = 1580
  val h = 400
  val detail = 100//%

  def hub(scene: String, middle: String) = {
    import js.JSConverters._
    val west = js.Array(
      "CLE vs TOR(4:2),CLE vs ATL(4:0),CLE vs DET(4:0)".split(",").toJSArray,
      "CLE vs TOR(4:2),CLE vs ATL(4:0),ATL vs BOS(4:2)".split(",").toJSArray,
      "CLE vs TOR(4:2),TOR vs MIA(4:3),MIA vs CHA(4:3)".split(",").toJSArray,
      "CLE vs TOR(4:2),TOR vs MIA(4:3),TOR vs IND(4:3)".split(",").toJSArray)

    val east = js.Array(
      "GSW vs OKC(4:3),GSW vs POR(4:1),GSW vs HOU(4:1)".split(",").toJSArray,
      "GSW vs OKC(4:3),GSW vs POR(4:1),POR vs LAC(4:2)".split(",").toJSArray,
      "GSW vs OKC(4:3),OKC vs SAS(4:2),OKC vs DAL(4:1)".split(",").toJSArray,
      "GSW vs OKC(4:3),OKC vs SAS(4:2),SAS vs MEM(4:0)".split(",").toJSArray)

    val paper = js.Dynamic.global.Raphael(scene, w, h)
    js.Dynamic.global.leftTree(west, middle, detail, scene, w, h, paper)
    js.Dynamic.global.rightTree(east, middle, detail, scene, w, h, paper)
  }

  @JSExport
  def main(scene: String): Unit = {
    hub(scene, "GSW(3) vs CLE(4)")
  }
}
