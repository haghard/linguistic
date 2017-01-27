package linguistic.tree

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

//http://stackoverflow.com/questions/28069561/dynamic-calls-to-javascript-in-scala-js
//https://www.jasondavies.com/wordtree/?source=obama-war-speech.txt&prefix=Iraq&reverse=0
//https://github.com/scala-js/scala-js-dom/blob/master/example/src/main/scala/example/Example.scala

@JSExport
object ObamaSpeechModule {
  val w = 1280
  val h = 400
  val detail = 100 // percent

  def tree(where: String, keyWord: String) {
    import js.JSConverters._
    val speech = js.Array(
      "must act, knowing that our work will be imperfect.".split("\\s").toJSArray,
      "must act, knowing that today’s victories will be only partial.".split("\\s").toJSArray,
      "will seize it – so long as we seize it together.".split("\\s").toJSArray,
      "will respond to the threat of climate change.".split("\\s").toJSArray,
      "will maintain our economic vitality and our national treasure.".split("\\s").toJSArray,
      "will defend our people and uphold our values through strength of arms and rule of law.".split("\\s").toJSArray,
      "have never relinquished our skepticism of central authority".split("\\s").toJSArray,
      "have always understood that when times change".split("\\s").toJSArray,
      "have lost, know too well the price that is paid for liberty.".split("\\s").toJSArray,
      "all define liberty in exactly the same way.".split("\\s").toJSArray,
      "all make to the flag that waves above and that fills our hearts with pride.".split("\\s").toJSArray

      //"have been waiting for you  for an hour".split("\\s").toJSArray,
      //"have been learning this subject for a year".split("\\s").toJSArray
    )

    val paper = js.Dynamic.global.Raphael(where, w, h)
    js.Dynamic.global.leftTree(speech, keyWord, detail, where, w, h, paper)
  }

  @JSExport
  def main(where: String): Unit = {
    tree(where, "We")
    //raphaeljs.Raphael("raphael", where, 200, {})
  }
}
