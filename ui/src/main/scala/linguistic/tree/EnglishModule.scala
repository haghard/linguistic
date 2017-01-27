package linguistic.tree

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object EnglishModule {
  val w = 1280
  val	h = 500
  val	detail = 100// percent

  def draw(where: String, keyWord: String) {
    import js.JSConverters._

    val lefts = js.Array(
      "I was coming home".split("\\s").toJSArray,
      "He was coming home from  work".split("\\s").toJSArray,
      "I  was coming out of the shops".split("\\s").toJSArray,
      "While I was going home".split("\\s").toJSArray,
      "She was coming  home".split("\\s").toJSArray)

    val rights = js.Array(
      "when a rabbit ate a mouse.".split("\\s").toJSArray,
      "but  a rabbit ate a rat.".split("\\s").toJSArray,
      "then a rat ate a mouse.".split("\\s").toJSArray,
      "the rat ate a mouse.".split("\\s").toJSArray,
      "and the cat ate a mouse.".split("\\s").toJSArray,
      "when the cat ate a mouse.".split("\\s").toJSArray,
      "when the cat ate the mouse.".split("\\s").toJSArray)

    val q = js.Array(
      "Can I borrow your".split("\\s").toJSArray,
      "Should I borrow your".split("\\s").toJSArray,
      "May I borrow your".split("\\s").toJSArray,
      "Might I borrow your".split("\\s").toJSArray,
      "Had I borrow your".split("\\s").toJSArray,
      "What if I borrow your".split("\\s").toJSArray
    )

    val paper = js.Dynamic.global.Raphael(where, w, h)

    /*
    val leftsA: js.Array[js.Array[String]] = lefts.map(_.reverse)
    js.Dynamic.global.leftTree(rights, keyWord, detail, where, w, h, paper)
    js.Dynamic.global.rightTree(leftsA, keyWord, detail, where, w, h, paper)
    */

    js.Dynamic.global.rightTree(q.map(_.reverse), keyWord, detail, where, w, h, paper)

  }

  @JSExport
  def main(where: String): Unit = {
    draw(where,"imagination")
    //draw(where, "yesterday")
    //raphaeljs.Raphael("raphael", where, 200, {})
  }
}
