package linguistic

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//https://github.com/mauricio/scala-sandbox/blob/master/src/main/scala/trie/Trie.scala
package object trie {

  object Trie {
    def apply(): Trie = new TrieNode()
  }

  sealed trait Trie extends Traversable[String] {

    def append(key: String): Unit

    def findByPrefix(prefix: String): scala.collection.Seq[String]

    def contains(word: String): Boolean

    def remove(word: String): Boolean

  }

  private[trie] class TrieNode(val char: Option[Char] = None, var word: Option[String] = None) extends Trie {

    private[trie] val children: mutable.Map[Char, TrieNode] =
      new java.util.TreeMap[Char, TrieNode]().asScala

    override def append(key: String) = {
      @tailrec def go(node: TrieNode, currentIndex: Int): Unit =
        if (currentIndex == key.length) node.word = Some(key)
        else {
          val pref   = key.charAt(currentIndex).toLower
          val result = node.children.getOrElseUpdate(pref, new TrieNode(Some(pref)))
          go(result, currentIndex + 1)
        }

      go(this, 0)
    }

    override def foreach[U](f: String => U): Unit = {
      @tailrec def go(nodes: TrieNode*): Unit =
        if (nodes.size != 0) {
          nodes.foreach(node => node.word.foreach(f))
          go(nodes.flatMap(node => node.children.values): _*)
        }

      go(this)
    }

    override def findByPrefix(prefix: String): scala.collection.Seq[String] = {
      @tailrec def go(currentIndex: Int, node: TrieNode, items: ListBuffer[String]): ListBuffer[String] =
        if (currentIndex == prefix.length) {
          items ++ node
        } else {
          node.children.get(prefix.charAt(currentIndex).toLower) match {
            case Some(child) => go(currentIndex + 1, child, items)
            case None        => items
          }
        }

      go(0, this, new ListBuffer[String]())
    }

    override def contains(word: String): Boolean = {
      @tailrec def go(currentIndex: Int, node: TrieNode): Boolean =
        if (currentIndex == word.length) {
          node.word.isDefined
        } else {
          node.children.get(word.charAt(currentIndex).toLower) match {
            case Some(child) => go(currentIndex + 1, child)
            case None        => false
          }
        }

      go(0, this)
    }

    override def remove(word: String): Boolean = {

      def loop(index: Int, continue: Boolean, path: ListBuffer[TrieNode]): Unit =
        if (index > 0 && continue) {
          val current = path(index)
          if (current.word.isDefined) loop(index, false, path)
          else {
            val parent = path(index - 1)
            if (current.children.isEmpty) {
              parent.children.remove(word.charAt(index - 1).toLower)
            }
            loop(index - 1, true, path)
          }
        }

      pathTo(word) match {
        case Some(path) => {
          var index    = path.length - 1
          var continue = true

          path(index).word = None
          //
          while (index > 0 && continue) {
            val current = path(index)

            if (current.word.isDefined) continue = false
            else {
              val parent = path(index - 1)
              if (current.children.isEmpty) {
                parent.children.remove(word.charAt(index - 1).toLower)
              }
              index -= 1
            }
          }

          true
        }
        case None => false
      }

    }

    private[trie] def pathTo(word: String): Option[ListBuffer[TrieNode]] = {
      def go(buffer: ListBuffer[TrieNode], currentIndex: Int, node: TrieNode): Option[ListBuffer[TrieNode]] =
        if (currentIndex == word.length) {
          node.word.map(word => buffer += node)
        } else {
          node.children.get(word.charAt(currentIndex).toLower) match {
            case Some(found) => {
              buffer += node
              go(buffer, currentIndex + 1, found)
            }
            case None => None
          }
        }

      go(new ListBuffer[TrieNode](), 0, this)
    }

    override def toString(): String = s"Trie(char=${char},word=${word})"
  }
}
