package linguistic.ps

import java.net.InetAddress
import org.isarnproject.collections.mixmaps.nearest.NearestMap
import org.isarnproject.collections.mixmaps.ordered.OrderedSet

object NearestWordEntity {

  // import org.isarnproject.collections.mixmaps.nearest._

  implicit val ipV4Address = new Numeric[String] {
    val size                                        = 4
    override def plus(x: String, y: String): String = ???

    override def toDouble(x: String): Double = ???

    override def toFloat(x: String): Float = ???

    override def toInt(x: String): Int = ???

    override def negate(x: String): String = ???

    override def fromInt(x: Int): String = ???

    override def toLong(x: String): Long = ???

    override def times(x: String, y: String): String = ???

    override def minus(x: String, y: String): String = ???

    override def compare(x: String, y: String): Int = {
      val xOctects = InetAddress.getByName(x).getHostAddress.toCharArray
      val yOctects = InetAddress.getByName(y).getHostAddress.toCharArray
      require((xOctects.size == yOctects.size) && (yOctects.size == size), "Address should match ipv4 schema")

      def divergedIndex(a: Array[Int], b: Array[Int]): Option[Int] = {
        @scala.annotation.tailrec
        def loop(start: Int, end: Int): Option[Int] = {
          val mid = start + (end - start) / 2
          println("look at " + mid)
          if (start > end) None
          else if (a(mid) == b(mid)) loop(mid + 1, end)
          else Some(mid)
        }
        if (a.size >= 1 && b.size >= 1 && a(0) != b(0)) Some(0)
        else loop(0, math.min(a.length, b.length) - 1)
      }

      def intersect(a: Array[Int], b: Array[Int]): Int = {
        var i_a = 0; var i_b = 0
        // new scala.collection.mutable.ArrayBuffer[Int]()
        val result        = new scala.collection.mutable.ListBuffer[Int]()
        var divergedIndex = 0
        while (i_a < a.size && i_b < b.size)
          if (a(i_a) < b(i_b)) {
            i_a += 1
          } else if (b(i_b) < a(i_a)) {
            i_b += 1
          } else {
            result += a(i_a)
            divergedIndex += 1
            i_a += 1
            i_b += 1
          }
        divergedIndex
      }

      def tryCompary(x: Array[Char], y: Array[Char], i: Int = 0): Int = {
        val a = x(i).toInt
        val b = y(i).toInt
        if (i < size)
          if (a < b) -1 else if (a > b) 1 else tryCompary(x, y, i + 1)
        else 0
      }

      tryCompary(xOctects, yOctects, 0)
    }
  }

  val zero: NearestMap[String, Int] = NearestMap.key[String].value[Int].empty
  val ips = zero + ("127.0.0.1" -> 0) + ("127.0.0.2" -> 1) + ("127.0.0.3" -> 2) + ("127.0.0.4" -> 3) +
    ("127.0.0.5" -> 4) + ("127.0.0.6" -> 5) + ("127.0.0.7" -> 6)

  ips.nearest("127.0.0.3")

}
