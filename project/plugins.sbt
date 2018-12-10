resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.7") // 1.0.8 depends on scalaJs 1.0.0-M3
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")