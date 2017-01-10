package linguistic

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.collection.immutable

object WordsSearchProtocol {

  trait Search {
    def keyword: String
  }
  case class SearchWord(keyword: String, maxResults: Int) extends Search
  case class SearchHomophones(keyword: String, maxResults: Int) extends Search

  abstract class Results {
    def strict: immutable.Seq[String]
    def source: Source[String, NotUsed] = Source(strict)
  }

  final case class SearchResults(strict: immutable.Seq[String]) extends Results

  final case class SearchFailed(ex: Exception) extends Results {
    override def strict: immutable.Seq[String] = throw ex
    override def source: Source[String, NotUsed] = Source.failed(ex)
  }

  case object IndexingCompleted
}
