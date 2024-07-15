import _root_.sbtdocker.DockerPlugin.autoImport._
import sbt._
import sbtdocker.ImageName

import scala.sys.process.Process

val scalaV          = "2.12.10" //10
val akkaVersion     = "2.6.21"
val AkkaHttpVersion = "10.2.7"
val version         = "0.3"

name := "linguistic"

lazy val java17Settings = Seq(
  "-XX:+UseZGC", // https://www.baeldung.com/jvm-zgc-garbage-collector
  "--add-opens",
  "java.base/java.nio=ALL-UNNAMED",
  "--add-opens",
  "java.base/sun.nio.ch=ALL-UNNAMED"
)

lazy val server = (project in file("server"))
  .settings(
    resolvers ++= Seq(
      "Typesafe Snapshots".at("https://repo.typesafe.com/typesafe/snapshots/"),
      "Secured Central Repository".at("https://repo1.maven.org/maven2"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.jcenterRepo,
      "Hyperreal Repository" at "https://dl.bintray.com/edadma/maven",
      "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
    ),

    /*resolvers ++= Seq(
        "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
        "isarn project" at "https://dl.bintray.com/isarn/maven/",
      ),*/
    scalacOptions in (Compile, console) := Seq(/*"-release:17",*/"-feature", "-Xfatal-warnings", "-deprecation", "-unchecked"),
    scalaVersion := scalaV,
    scalaJSProjects := Seq(ui),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    //Compile / compile := (Compile / compile).dependsOn(scalaJSPipeline, cpCss()).value,
    compile in Compile := (compile in Compile).dependsOn(scalaJSPipeline, cpCss).value,

    name := "server",
    //javaOptions in runMain := Seq("ENV=development", "CONFIG=./server/conf"),
    runMain / fork := true,
    run / fork := true,
    run / javaOptions ++= /*java17Settings,*/ Seq("-XX:+PrintCommandLineFlags", "-XshowSettings:system -version", "-Xmx1700m", "-XX:+UseZGC"),
    //javaOptions in runMain := Seq("-XX:+PrintFlagsFinal", "-Xms756m", "-Xmx1256m"),
    //javaOptions ++= java17Settings,
    libraryDependencies ++= Seq(
        "ch.qos.logback"         % "logback-classic"         % "1.1.2",
        "org.mindrot"            % "jbcrypt"                 % "0.4",
        "com.vmunier"           %% "scalajs-scripts"         % "1.1.0", //Twirl templates to link Scala.js output scripts into a HTML page.
        "com.lihaoyi"           %% "scalatags"               % "0.6.7",
        "org.webjars"            % "bootstrap"               % "3.3.6",
        //"com.datastax.cassandra" % "cassandra-driver-extras" % "3.11.5",
        "com.jsuereth"          %% "scala-arm"               % "2.0",

        //https://github.com/Odomontois/ScalaSuffixArray/blob/master/src/test/scala/odo/sufixarr/Test.scala

        "org.openjdk.jol"  %  "jol-core"        % "0.17",
        //https://www.garretwilson.com/blog/2011/12/15/suffix-trees-java

        "org.isarnproject" %% "isarn-collections" % "0.0.4",
        "org.isarnproject" %% "isarn-sketches" % "0.3.0",

        "com.rklaehn"   %% "radixtree" % "0.5.1",
      
        //https://github.com/edadma/b-tree
        //https://github.com/edadma/b-tree/blob/master/src/test/scala/FileSpecificTests.scala
        //"xyz.hyperreal" %% "b-tree" % "0.5",

        "org.wvlet.airframe" %% "airframe-ulid" % "24.6.0",
        "ru.odnoklassniki" % "one-nio" % "1.7.3",
        /*
          val map = new one.nio.mem.LongObjectHashMap[Array[Byte]](5) //5kv only possible
          map.put(0L, Hex.parseBytes("1111aaaa"))
          map.put(1L, Hex.parseBytes("11111bbb"))
        */
      
        "com.graphhopper" % "graphhopper-core" % "8.0", //"7.0", 9.1


        //"net.openhft" % "chronicle-map" % "3.25ea6", //.8


        //https://github.com/rohansuri/adaptive-radix-tree
        "com.github.rohansuri" % "adaptive-radix-tree" % "1.0.0-beta",

        "org.scalatest" %% "scalatest" % "3.0.4" % "test"
      ) ++ Seq(
        ("com.softwaremill.akka-http-session" %% "core"                       % "0.7.1").exclude("com.typesafe.akka", "akka-http"),

        "com.typesafe.akka" %% "akka-actor-typed"            % akkaVersion,
        "com.typesafe.akka" %% "akka-protobuf"               % akkaVersion,
        "com.typesafe.akka" %% "akka-protobuf-v3"            % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding"       % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-metrics"        % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j"                  % akkaVersion,
        "com.typesafe.akka" %% "akka-discovery"              % akkaVersion,
        "com.typesafe.akka" %% "akka-distributed-data"       % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence"            % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence-query"      % akkaVersion,
        "com.typesafe.akka" %% "akka-persistence-typed"      % akkaVersion,
        "com.typesafe.akka" %% "akka-actor"                  % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster"                % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-coordination"           % akkaVersion,
        "com.typesafe.akka" %% "akka-stream"                 % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-tools"          % akkaVersion,
        "com.typesafe.akka" %% "akka-http"                   % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-core"              % AkkaHttpVersion,
        "com.typesafe.akka" %% "akka-http-spray-json"        % AkkaHttpVersion,

        ("com.typesafe.akka"                  %% "akka-persistence-cassandra" % "1.1.1")
          .excludeAll(
            ExclusionRule(organization = "io.netty", name = "netty-all")
          ), //to exclude netty-all-4.1.39.Final.jar

        ("com.lihaoyi" % "ammonite" % "2.5.0" % "test").cross(CrossVersion.full)
      ),

    dependencyOverrides ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"            % akkaVersion,
      "com.typesafe.akka" %% "akka-protobuf"               % akkaVersion,
      "com.typesafe.akka" %% "akka-protobuf-v3"            % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding"       % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics"        % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"                  % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery"              % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data"       % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence"            % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query"      % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed"      % akkaVersion,
      "com.typesafe.akka" %% "akka-actor"                  % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"                % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-coordination"           % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"                 % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"          % akkaVersion,
      "com.typesafe.akka" %% "akka-http"                   % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-core"              % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"        % AkkaHttpVersion
    ),

    //javaOptions in runMain += "-DENV=prod",

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
      case PathList(xs @ _*) if xs.last == "io.netty.versions.properties" ⇒ MergeStrategy.rename
      case other                                                          ⇒ (assemblyMergeStrategy in assembly).value(other)
    },
    imageNames in docker := Seq(ImageName(namespace = Some("haghard"), repository = "linguistic", tag = Some(version))),
    buildOptions in docker := BuildOptions(
        cache = false,
        removeIntermediateContainers = BuildOptions.Remove.Always,
        pullBaseImage = BuildOptions.Pull.Always
      ),
    //envVars := Map("-DENV" -> "development", "-DCONFIG" -> "./server/conf"),

    //sbt -Denv=...  -Dconfig=...
    /*
  envVars in runMain := Map(
    "ENV" -> sys.props.getOrElse("env", "development"),
    "CONFIG" -> sys.props.getOrElse("config", "./server/conf")
  ),
     */

    //docker -Denv="development"
    docker / dockerfile := {
      //possible envs: development | production
      val appEnv = sys.props.getOrElse("env", "production")

      println(s"★ ★ ★ ★ ★ ★ ★ ★ ★ Build Docker image for Env:$appEnv ★ ★ ★ ★ ★ ★ ★ ★ ★")

      //val appConfig = "/app/conf"
      val baseDir        = baseDirectory.value
      val artifact: File = assembly.value

      val imageAppBaseDir       = "/app"
      val configDir             = "conf"
      val artifactTargetPath    = s"$imageAppBaseDir/${artifact.name}"
      val artifactTargetPath_ln = s"$imageAppBaseDir/${appEnv}-${name.value}.jar"
      val jksTargetPath         = s"$imageAppBaseDir/linguistic.jks"

      val dockerResourcesDir        = baseDir / "docker-resources"
      val dockerResourcesTargetPath = s"$imageAppBaseDir/"

      val jks = baseDir / "ser" / "linguistic.jks"

      val prodConfigSrc = baseDir / "conf" / "production.conf"
      val devConfigSrc  = baseDir / "conf" / "development.conf"

      val appProdConfTarget = s"$imageAppBaseDir/$configDir/production.conf"
      val appDevConfTarget  = s"$imageAppBaseDir/$configDir/development.conf"

      new sbtdocker.mutable.Dockerfile {
        from("adoptopenjdk:11")
        //from("adoptopenjdk/openjdk11:jdk-11.0.2.9")
        //from("openjdk:10-jre")
        //from("openjdk:8-jre")
        //from("openjdk:8u131")
        maintainer("haghard")

        env("VERSION", version)
        env("APP_BASE", imageAppBaseDir)
        env("CONFIG", s"$imageAppBaseDir/$configDir")

        env("ENV", appEnv)

        workDir(imageAppBaseDir)

        copy(artifact, artifactTargetPath)
        copy(dockerResourcesDir, dockerResourcesTargetPath)
        copy(jks, jksTargetPath)

        copy(baseDir / "data" / "words.txt", s"$imageAppBaseDir/words.txt")
        copy(baseDir / "data" / "homophones.txt", s"$imageAppBaseDir/homophones.txt")

        if (prodConfigSrc.exists)
          copy(prodConfigSrc, appProdConfTarget) //Copy the prod config

        if (devConfigSrc.exists)
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
  )
  .enablePlugins(SbtWeb, sbtdocker.DockerPlugin)
  .dependsOn(sharedJvm, protocol)

//for debugging
def cpCss() =
  baseDirectory map { dir ⇒
    def execute() = {
      //IO.copyFile(dir / "src" / "main" / "resources" / "main.css", dir / "target" /"web"/"web-modules"/"main"/"webjars"/"lib"/"bootstrap"/"css"/"main.css")
      //IO.copyFile(dir / "src" / "main" / "resources" / "chat.css", dir / "target"/"web"/"web-modules"/"main"/"webjars"/"lib"/"bootstrap"/"css"/"chat.css")
      Process(s"cp ${dir}/src/main/resources/main.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
      Process(s"cp ${dir}/src/main/resources/chat.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
    }
    println("Coping resources ...")
    haltOnCmdResultError(execute())
  }

def haltOnCmdResultError(result: Int) {
  if (result != 0) throw new Exception("Build failed")
}

lazy val ui = (project in file("ui"))
  .settings(
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    scalaVersion := scalaV,
    scalaJSUseMainModuleInitializer := false,
    scalaJSUseMainModuleInitializer in Test := false,
    libraryDependencies ++= Seq(
        //"org.singlespaced" %%% "scalajs-d3" % "0.3.4",
        "com.github.japgolly.scalajs-react" %%% "core"  % "0.11.5", //"1.4.2", //"0.11.3",
        "com.github.japgolly.scalajs-react" %%% "extra" % "0.11.5" //"0.11.5"
      ),
    jsDependencies ++= Seq(
        "org.webjars"       % "jquery" % "2.1.4" / "2.1.4/jquery.js",
        "org.webjars.bower" % "react"  % "15.6.1"
        / "react-with-addons.js"
        minified "react-with-addons.min.js"
        commonJSName "React",
        "org.webjars.bower" % "react" % "15.6.1"
        / "react-dom.js"
        minified "react-dom.min.js"
        dependsOn "react-with-addons.js"
        commonJSName "ReactDOM",
        "org.webjars.bower" % "react" % "15.6.1"
        / "react-dom-server.js"
        minified "react-dom-server.min.js"
        dependsOn "react-dom.js"
        commonJSName "ReactDOMServer"
      )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

scalafmtOnCompile := true

lazy val shared = sbtcrossproject.CrossPlugin.autoImport
  .crossProject(JSPlatform, JVMPlatform)
  .crossType(sbtcrossproject.CrossType.Pure)
  .settings(
    scalaVersion := scalaV,
    libraryDependencies ++= Seq("com.lihaoyi" %%% "upickle" % "0.6.6")
  )
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs  = shared.js

lazy val protocol = (project in file("protocol"))
  .settings(
    name := "protocol",
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
      )
  )
  .settings(
    PB.targets in Compile := Seq(scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value / "protobuf")
  )

addCommandAlias("c", "compile")
addCommandAlias("r", "reload")


//onLoad in Global := (onLoad in Global).value andThen {s: State => "project server" :: s}