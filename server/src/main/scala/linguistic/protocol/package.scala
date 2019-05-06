package linguistic

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable

package object protocol {
  case object IndexingCompleted

  trait SearchQuery {
    def keyword: String
  }

  case class WordsQuery(keyword: String, maxResults: Int) extends SearchQuery

  case class HomophonesQuery(keyword: String, maxResults: Int) extends SearchQuery

  abstract class Results {
    def strict: immutable.Seq[String]

    def source: Source[String, NotUsed] = Source(strict)
  }

  case class SearchResults(strict: immutable.Seq[String]) extends Results

  case class Words(entry: Seq[String])

  case class Homophones(homophones: Seq[Homophone])

  case class Homophone(key: String, homophones: Seq[String])
}
