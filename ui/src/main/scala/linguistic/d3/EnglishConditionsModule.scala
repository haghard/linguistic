package linguistic.d3

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object EnglishConditionsModule {

  val w = 1280
  val h = 150
  val detail = 100 // percent

  //http://www.metrolyrics.com/if-i-had-a-million-dollars-lyrics-barenaked-ladies.html
  def draw(where: String, keyWord: String) {
    import js.JSConverters._

    val items = js.Array(
      "If I could I would.".split("\\s").toJSArray,
      "If I had a million dollars Maybe we could put a little tiny fridge.".split("\\s").toJSArray,
      "If I had a million dollars you could help.It wouldn't be that hard.".split("\\s").toJSArray,
      "If I had a million dollars I would buy you a house.".split("\\s").toJSArray,
      "If I had a magic wand I would turn him into Jack Nicholson.".split("\\s").toJSArray,
      "If I were you I would run away.".split("\\s").toJSArray,
      "If I had known I would have told you.".split("\\s").toJSArray,
      "If I had had money that day I would have given them to you.".split("\\s").toJSArray,
      "If I had had money that day I would have bought that book.".split("\\s").toJSArray)


    val paper = js.Dynamic.global.Raphael(where, w, h)
    js.Dynamic.global.leftTree(items, keyWord, detail, where, w, h, paper)
  }

  @JSExport
  def main(scene: String): Unit = {
    draw(scene, "")
  }
}
