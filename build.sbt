import _root_.sbtdocker.DockerPlugin.autoImport._
import sbt._
import com.typesafe.sbt.packager.docker.Dockerfile
import sbtdocker.ImageName

import scala.util.Try

val scalaV = "2.11.8"
val akkaVersion = "2.4.16"
val version = "0.1"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

name := "linguistic"

//val appStage = settingKey[String]("stage")
//appStage := sys.props.getOrElse("stage", "development")

updateOptions in Global := updateOptions.in(Global).value.withCachedResolution(true)

lazy val server = (project in file("server")).settings(
  resolvers ++= Seq(
    "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
    "isarn project" at "https://dl.bintray.com/isarn/maven/"),

  scalacOptions in(Compile, console) := Seq("-feature", "-Xfatal-warnings", "-deprecation", "-unchecked"),
  scalaVersion := scalaV,
  scalaJSProjects := Seq(ui),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile <<= (compile in Compile).dependsOn(scalaJSPipeline, cpCss),

  name := "server",

  //javaOptions in runMain := Seq("ENV=development", "CONFIG=./server/conf"),

  fork in runMain := true,

  libraryDependencies ++= Seq(
    ("com.softwaremill.akka-http-session" %% "core" % "0.3.0").exclude("com.typesafe.akka", "akka-http"),
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "com.vmunier" %% "scalajs-scripts" % "1.1.0", //Twirl templates to link Scala.js output scripts into a HTML page.
    "com.lihaoyi" %% "scalatags"  % "0.6.2",
    "org.webjars" % "bootstrap" % "3.3.6",
    "com.datastax.cassandra" % "cassandra-driver-extras" % "3.1.0",
    "com.jsuereth"           %% "scala-arm"              % "1.4",
    "org.openjdk.jol"        %  "jol-core"               % "0.6",
    "com.rklaehn"            %% "radixtree"              % "0.4.0", //blocker for scala 2.12
    "org.scalatest"          %% "scalatest"              % "3.0.1" % "test"
  ) ++ Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.22"),

  //javaOptions in runMain += "-DENV=prod",

  //exclude("com.typesafe.akka", "akka-http")

  /*
  javaOptions in Universal ++= Seq(
    // -J params will be added as jvm parameters
    "-J-Xmn1G",
    "-J-Xms1G",
    "-J-Xmx3G",
    // others will be added as app parameters
    "-Dlog4j.configurationFile=conf/log4j2.xml"
  ),
*/

  WebKeys.packagePrefix in Assets := "public/",

  (managedClasspath in Runtime) += (packageBin in Assets).value,

  mainClass in assembly := Some("linguistic.Application"),

  assemblyJarName in assembly := s"linguistic-${version}.jar",

  // Resolve duplicates for Sbt Assembly
  assemblyMergeStrategy in assembly := {
    case PathList(xs@_*) if xs.last == "io.netty.versions.properties" => MergeStrategy.rename
    case other => (assemblyMergeStrategy in assembly).value(other)
  },

  imageNames in docker := Seq(ImageName(namespace = Some("haghard"), repository = "linguistic", tag = Some(version))),

  buildOptions in docker := BuildOptions(cache = false,
    removeIntermediateContainers = BuildOptions.Remove.Always,
    pullBaseImage = BuildOptions.Pull.Always),

  //envVars := Map("-DENV" -> "development", "-DCONFIG" -> "./server/conf"),

  //sbt -Denv=...  -Dconfig=...
  /*
  envVars in runMain := Map(
    "ENV" -> sys.props.getOrElse("env", "development"),
    "CONFIG" -> sys.props.getOrElse("config", "./server/conf")
  ),
  */

  dockerfile in docker := {
    val appEnv = try { System.getenv("env") } catch { case e: Throwable => "production" }
    //sys.props.getOrElse("env", "production")
    val appConfig = "/app/conf"
    //sys.props.getOrElse("config", "/app/conf")

    val baseDir = baseDirectory.value
    val artifact: File = assembly.value

    val imageAppBaseDir = "/app"
    val configDir = "conf"
    val artifactTargetPath = s"$imageAppBaseDir/${artifact.name}"
    val artifactTargetPath_ln = s"$imageAppBaseDir/${appEnv}-${name.value}.jar"
    val jksTargetPath = s"$imageAppBaseDir/linguistic.jks"

    val dockerResourcesDir = baseDir / "docker-resources"
    val dockerResourcesTargetPath = s"$imageAppBaseDir/"

    val jks = baseDir / "ser" / "linguistic.jks"

    //val prodConfigSrc = baseDir / "src" / "main" / "resources" / "production.conf"

    val prodConfigSrc = baseDir / "conf" / "production.conf"
    val devConfigSrc =  baseDir / "conf" / "development.conf"

    //val curEnv = System.getenv("ENV") //development | production

    val appProdConfTarget = s"$imageAppBaseDir/$configDir/production.conf"
    val appDevConfTarget = s"$imageAppBaseDir/$configDir/development.conf"

    new sbtdocker.mutable.Dockerfile {
      from("openjdk:8-jre")
      maintainer("haghard")

      env("VERSION", version)
      env("APP_BASE", imageAppBaseDir)
      env("CONFIG", s"$imageAppBaseDir/$configDir")

      env("ENV", appEnv)

      //workDir(imageAppBaseDir)
      //run("mkdir", configDir)

      workDir(imageAppBaseDir)

      copy(artifact, artifactTargetPath)
      copy(dockerResourcesDir, dockerResourcesTargetPath)
      copy(jks, jksTargetPath)

      if(prodConfigSrc.exists)
        copy(prodConfigSrc, appProdConfTarget) //Copy the prod config

      if(devConfigSrc.exists)
        copy(devConfigSrc, appDevConfTarget) //Copy the prod config

      runRaw(s"ls $appProdConfTarget")
      runRaw(s"ls $appDevConfTarget")

      runRaw(s"cd $configDir && ls -la && cd ..")
      runRaw("ls -la")

      //Symlink the service jar to a non version specific name
      run("ln", "-sf", s"$artifactTargetPath", s"$artifactTargetPath_ln")

      entryPoint(s"${dockerResourcesTargetPath}docker-entrypoint.sh")
    }
  }

).enablePlugins(SbtWeb, SbtTwirl, JavaAppPackaging, sbtdocker.DockerPlugin).dependsOn(sharedJvm)

//for debugging
def cpCss() = (baseDirectory) map { dir =>
  def execute() = {
    Process(s"cp ${dir}/src/main/twirl/linguistic/main.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
    Process(s"cp ${dir}/src/main/resources/chat.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
    Process(s"cp ${dir}/src/main/resources/nv.d3.js ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/js").!
    Process(s"cp ${dir}/src/main/twirl/linguistic/nv.d3.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
  }

  println("Coping resources ...")
  haltOnCmdResultError(execute())
}

def haltOnCmdResultError(result: Int) {
  if (result != 0) throw new Exception("Build failed")
}

lazy val ui = (project in file("ui")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.singlespaced" %%% "scalajs-d3" % "0.3.3", //the version for scala 2.12 is 0.3.4
    "com.github.japgolly.scalajs-react" %%% "core"    % "0.11.3",
    "com.github.japgolly.scalajs-react" %%% "extra"   % "0.11.3"
  ),

  jsDependencies ++= Seq(
    //"org.webjars" % "jquery" % "2.1.3" / "2.1.3/jquery.js",
    "org.webjars.bower" % "react" % "15.3.2"
        /        "react-with-addons.js"
        minified "react-with-addons.min.js"
        commonJSName "React",

      "org.webjars.bower" % "react" % "15.3.2"
        /         "react-dom.js"
        minified  "react-dom.min.js"
        dependsOn "react-with-addons.js"
        commonJSName "ReactDOM",

      "org.webjars.bower" % "react" % "15.3.2"
        /         "react-dom-server.js"
        minified  "react-dom-server.min.js"
        dependsOn "react-dom.js"
        commonJSName "ReactDOMServer"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %%% "upickle" % "0.4.3"
    )
  ).jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

//cancelable in Global := true