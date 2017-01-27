package linguistic.tree

import org.singlespaced.d3js.d3.Primitive
import org.singlespaced.d3js.{SimpleLink, Tree, d3}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@js.native
trait AnimalNode extends js.Object {
  var id: js.UndefOr[Int] = js.native
  var x: js.UndefOr[Int] = js.native
  var y: js.UndefOr[Int] = js.native
  val depth: Int = js.native
  val parent: String = js.native
  val name: String = js.native
  val children: js.Array[AnimalNode] = js.native
}

@JSExport
object AnimalsModule {

  val width = 800.0
  val height = 600.0
  val marginLeft = 0.0
  val marginTop = 30.0
  val distance = 100

  @JSExport
  def main(): Unit = {
    draw
  }

  private def draw = {
    d3.json("json-example", (error: js.Any, json: js.Any) => {

      val jsonTypedFromFile = json.asInstanceOf[AnimalNode]

      val svg = d3.select("#content")
        .append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", "translate(" + marginLeft + "," + marginTop + ")")

      val tupledDimensions = (width, height)

      val animalNodeTree: Tree[AnimalNode] = d3.layout.tree().size(tupledDimensions)
      val animalNodes: js.Array[AnimalNode] = animalNodeTree.nodes(jsonTypedFromFile)
      val untypedLinks: js.Array[_ <: Any] = animalNodeTree.links(animalNodes)

      val animalNodeLinks = untypedLinks.map(link => {
        val linkObj = link.asInstanceOf[js.Dynamic]
        SimpleLink(linkObj.source.asInstanceOf[AnimalNode], linkObj.target.asInstanceOf[AnimalNode])
      })

      // Normalize for fixed-depth.
      animalNodes.foreach { node: AnimalNode =>
        node.y = node.depth * distance
        //println(node.y)
      }

      var nodeCount: Int = 0

      val node: org.singlespaced.d3js.selection.Update[AnimalNode] =
        svg.selectAll("g.node").data(animalNodes, (node: AnimalNode, index: Int) => {
          nodeCount += 1
          node.id = nodeCount
          node.id.toString
      })

      val nodeEnter: org.singlespaced.d3js.selection.Enter[AnimalNode] = node.enter()

      val nodeWithPosition = nodeEnter.append("g")
        .attr("class", "node")
        .attr("transform", (animalNode: AnimalNode, x: Int, y: js.UndefOr[Int]) => {
          println(animalNode.id)
          "translate(" + animalNode.x + "," + animalNode.y + ")": Primitive
        })

      nodeWithPosition.append("circle")
        .attr("r", 10)
        //.style("fill", d3.color("#edf8b1"))
        //rgb(vis.forceRep.colors(d.nodeValue.lang)).brighter())
        //.style("fill", "#fff")
        .style("fill", "#3FEE99") // "#7fcdbb") //none
        .style("stroke", "black")
        .style("stroke-width", "1px")

      nodeWithPosition.append("text")
        .attr("x", 13)
        .attr("dy", ".35em")
        .attr("text-anchor", "start")
        .text((node: AnimalNode, x: Int, y: js.UndefOr[Int]) => node.name)
        .style("fill-opacity", 1)

      val link = svg.selectAll("g.link")
        .data(animalNodeLinks, (link: SimpleLink[AnimalNode], index: Int) => { "" + link.target.id })
        //.attr("d", linkGenerator)

      link.enter()
        .insert("line", "g")
        .attr("class", "link")
        .style("stroke", "black")
        .attr("x1", (node: SimpleLink[AnimalNode], x: Int, y: js.UndefOr[Int]) => {
          "" + node.source.x.getOrElse(0.0): Primitive
        })
        .attr("y1", (node: SimpleLink[AnimalNode], x: Int, y: js.UndefOr[Int]) => {
          "" + node.source.y.getOrElse(0.0): Primitive
        })
        .attr("x2", (node: SimpleLink[AnimalNode], x: Int, y: js.UndefOr[Int]) => {
          "" + node.target.x.getOrElse(0.0): Primitive
        })
        .attr("y2", (node: SimpleLink[AnimalNode], x: Int, y: js.UndefOr[Int]) => {
          "" + node.target.y.getOrElse(0.0): Primitive
        })

      println("Done")
    }

    )
  }
}
