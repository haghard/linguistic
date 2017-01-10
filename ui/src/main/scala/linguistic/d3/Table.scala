package linguistic.d3

import org.singlespaced.d3js.Ops._
import org.singlespaced.d3js.d3

import scala.scalajs.js

object Table {
  def apply(selector: String) = {
    val matrix = js.Array(
      js.Array(11975, 5871, 8916, 2868),
      js.Array(1951, 10048, 2060, 6171),
      js.Array(8010, 16145, 8090, 8045),
      js.Array(1013, 990, 940, 6907))

    val tr = d3.select(selector).attr("id", "table1").append("table").selectAll("tr")
      .data(matrix)
      .enter().append("tr")

    val td = tr.selectAll("td")
      .data { (d: js.Array[Int]) =>
        //println(d);
        d
      }
      .enter().append("td")
      .text { (d: Int) => d.toString  }
  }
}