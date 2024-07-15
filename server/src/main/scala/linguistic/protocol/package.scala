package linguistic

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source

import scala.collection.{immutable, mutable}

package object protocol {
  case object IndexingCompleted

  trait SearchQuery {
    def keyword: String
    def maxResults: Int
    def replyTo: ActorRef[SearchResults]
  }

  object SearchQuery {
    final case class WordsQuery(keyword: String, maxResults: Int, replyTo: ActorRef[SearchResults]) extends SearchQuery

    final case class HomophonesQuery(keyword: String, maxResults: Int, replyTo: ActorRef[SearchResults]) extends SearchQuery
  }

  sealed trait Results {
    def strict: immutable.Seq[String]
    def source: Source[String, NotUsed] = Source.fromIterator(() => strict.iterator)
  }

  final case class SearchResults(strict: immutable.Seq[String]) extends Results


  final case class Homophones(homophones: Seq[Homophone])

  final case class Homophone(key: String, homophones: Seq[String])


  final case class UniqueTermsByShard(terms: Seq[String])
  final case class UniqueTermsByShard2(terms: mutable.HashSet[String])


  final case class AddOneWord(w: String)
  final case class OneWordAdded(w: String)

}
