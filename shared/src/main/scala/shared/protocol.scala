package shared

object protocol {

  case class SignInResponse(login: String, photo: String)

  import upickle.default._
  implicit def rw0: ReadWriter[SignInResponse] = macroRW
}
