package linguistic

import scala.collection._

trait AppSupport {
  val Opt = """(\S+)=(\S+)""".r

  def argsToOpts(args: Seq[String]): Map[String, String] =
    args.collect { case Opt(key, value) => key -> value }(breakOut)

  def applySystemProperties(options: Map[String, String]): Unit =
    for ((key, value) <- options if key startsWith "-D") {
      println(s"Set $key: $value")
      System.setProperty(key substring 2, value)
    }
}