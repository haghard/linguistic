package shared

object Headers {
  val fromClient = "Client-Auth-Token"
  val fromServer = "Server-Auth-Token"

  val SignInHeader = "X-Sign-in"

  val SignUpHeader = "X-Sign-up"

  //def auth(login: String) = s"${login}-auth-chatter-token"
  //def userToken(login: String) = s"${login}-chatter-user-token"
  //def timeLineProgressToken(login: String) = s"${login}-timeline-token"
}
