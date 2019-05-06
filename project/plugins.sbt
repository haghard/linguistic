resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.27")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.7") // 1.0.8 depends on scalaJs 1.0.0-M3
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.6.0-RC4")