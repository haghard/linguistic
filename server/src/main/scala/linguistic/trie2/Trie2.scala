package linguistic

import scala.collection.Seq
import scala.collection.immutable.{TreeMap, WrappedString}

//https://gist.github.com/timcowlishaw/1363652
package object trie2 {

  class Trie[V](key: Option[Char]) {

    def this() {
      this(None)
    }

    private var value: Option[V] = None
    private var nodes = new TreeMap[Char, Trie[V]]

    def put(k: String, v: V): Trie[V] = {
      normalise(k) match {
        case Seq() =>
          this.value = Some(v)
          this
        case Seq(h, t @ _*) => {
          val node = this.nodes.get(h) match {
            case None => {
              val n = new Trie[V](Some(h))
              this.nodes = this.nodes.insert(h, n)
              n
            }
            case Some(n) => n
          }
          node.put(t, v)
        }
      }
    }

    private def normalise(s: String): WrappedString = {
      new WrappedString(s.toLowerCase.replaceAll("""\W+""", ""))
    }

    def get(k: String): Option[V] = {
      nodeFor(k).flatMap { (n) => n.value }
    }

    def getAllWithPrefix(k: String): Seq[V] = {
      nodeFor(k).toList.flatMap { (n) => n.getAll }
    }

    def getAll: Seq[V] = {
      this.value.toList ++ this.nodes.values.flatMap { n => n.getAll }
    }

    override def toString = {
      key.toString ++ ":" ++ value.toString ++ "\n" ++ nodes.values.map { n => n.toString.split("\n")
        .map { l => "  " + l }.mkString("\n") }.mkString("\n")
    }

    private def nodeFor(k: String): Option[Trie[V]] = {
      normalise(k) match {
        case Seq() => Some(this)
        case Seq(h, t @ _*) => nodes.get(h).flatMap { n => n.nodeFor(t) }
      }
    }

    implicit def charSeq2WrappedString(s: Seq[Char]): WrappedString = {
      new WrappedString(s.mkString)
    }

    implicit def charSeq2String(s: Seq[Char]): String = {
      s.mkString
    }

    implicit def catOptions[A](xs: Seq[Option[A]]): Seq[A] = {
      xs.flatMap { (x) => x.toList }
    }
  }
}