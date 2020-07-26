resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.7")

//addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28")

addSbtPlugin("com.scalapenos" % "sbt-prompt" % "1.0.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

addSbtPlugin("de.heikoseeberger"  % "sbt-header"   % "5.0.0")

addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.9.0")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker"    % "1.5.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")