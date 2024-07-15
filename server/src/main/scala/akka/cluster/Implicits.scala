package akka.cluster

object Implicits {
  implicit class MemberOps(val member: Member) extends AnyVal {
    def details()           = s"${member.uniqueAddress}:${member.upNumber}"
    def addressWithIncNum() = s"${member.uniqueAddress.address}:${member.upNumber}"
  }
}
