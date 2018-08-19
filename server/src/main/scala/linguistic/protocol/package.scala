package linguistic

package object protocol {

  trait SearchQuery {
    def keyword: String
  }

  case class WordsQuery(keyword: String, maxResults: Int) extends SearchQuery

  case class HomophonesQuery(keyword: String, maxResults: Int) extends SearchQuery


  case class Words(entry: Seq[String])

  case class Homophones(homophones: Seq[Homophone])
  case class Homophone(key: String, homophones: Seq[String])
}
