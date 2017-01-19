package shared

object Routes {

  val pref = "api"

  val v1 = "v1.0"

  val signIn = "sign-in"

  val signUp = "sign-up"

  val search = "search"

  val searchHomophonesPath = "homophones"

  val searchWordsPath = "wordslist"

  val clientSignIn = s"$pref/$v1/$signIn"

  val clientSignUp = s"$pref/$v1/$signUp"

  private def search0(q: String, n: Int) = s"search?q=$q&n=$n"

  def search(searchType: String, q: String, n: Int) = s"${pref}/${v1}/${searchType}/${search0(q, n)}"

  val Separator = "&"
}