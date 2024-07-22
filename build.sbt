import _root_.sbtdocker.DockerPlugin.autoImport._
import sbt._
import sbtdocker.ImageName

import scala.sys.process.Process

//https://github.com/scala/scala/releases/tag/v2.12.10
val scalaV          = "2.12.10" //TUESDAY 10 SEPTEMBER 2019
val akkaVersion     = "2.6.21"
val AkkaHttpVersion = "10.2.10"
val AkkaMngVersion = "1.4.1"
val version         = "0.3"

name := "linguistic"

lazy val java17Settings = Seq(
  // https://www.baeldung.com/jvm-zgc-garbage-collector
  "--add-opens",
  "java.base/java.nio=ALL-UNNAMED",
  "--add-opens",
  "java.base/sun.nio.ch=ALL-UNNAMED"
)

val scalacSettings = Seq(
  scalacOptions ++= Seq(
    "-Xsource:2.13",
    "-feature",
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-deprecation",
    "-language:implicitConversions",

    /*
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-encoding", "UTF-8",                // Specify character encoding used by source files.
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
    */
  )
)

lazy val server = project
  .in(file("server"))
  .settings(scalacSettings)
  .settings(
    resolvers ++= Seq(
      "Typesafe Snapshots".at("https://repo.typesafe.com/typesafe/snapshots/"),
      "Secured Central Repository".at("https://repo1.maven.org/maven2"),
      Resolver.jcenterRepo,
      "Hyperreal Repository" at "https://dl.bintray.com/edadma/maven",
      "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
    ) ++ Resolver.sonatypeOssRepos("snapshots"),

    //scalacOptions in (Compile, console) := Seq("-release:17","-feature", "-Xfatal-warnings", "-deprecation", "-unchecked"),
    scalaVersion := scalaV,
    scalaJSProjects := Seq(ui),
    //pipelineStages in Assets := Seq(scalaJSPipeline),
    Assets / pipelineStages := Seq(scalaJSPipeline),
    Compile / compile := (Compile / compile).dependsOn(scalaJSPipeline, cpCss()).value,

    name := "server",

    //run / fork := false,
    run / fork := true,
    run / connectInput := true,

    //https://backstage.forgerock.com/knowledge/kb/article/a75965340
    /*
    -XX:+UseG1GC
    -XX:InitialHeapSize=2g
    -XX:MaxHeapSize=2g
    -XX:MaxMetaspaceSize=256m
    -XX:MaxGCPauseMillis=500
    -XX:+DisableExplicitGC
    -XX:+UseStringDeduplication
    -XX:+ParallelRefProcEnabled
    -XX:MaxTenuringThreshold=1
    -Xlog:gc=debug:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=100m
    */

    //https://www.linkedin.com/pulse/jdk-17-g1gc-vs-zgc-usage-core-exchange-application-performance-raza
    javaOptions ++= Seq(
      "-XX:+PrintCommandLineFlags",
      "-XshowSettings:system",
      "-XX:+UseStringDeduplication",
      "-XX:+UseCompressedOops", //32bit ref
      "-XX:NativeMemoryTracking=detail", //summary
      /*"-Xlog:gc* -version",*/
      "-Xlog:gc=debug:file=./gc.log:time,uptime,level,tags:filecount=5,filesize=100m",
      /*"-XX:ObjectAlignmentInBytes=8",*/
      "-Xmx1000m",
      "-Xms1000m",
      "-XX:-UseAdaptiveSizePolicy",   //heap never resizes
      "-XX:MaxDirectMemorySize=128m", //Will get a error if allocate more mem for direct byte buffers
      "-XX:+UseG1GC",

      //https://softwaremill.com/reactive-event-sourcing-benchmarks-part-2-postgresql/
      //"-XX:ActiveProcessorCount=4",

      //"-XX:+UseZGC"
    ) ++ java17Settings,

    //scalafmtOnCompile := true,

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
        "xyz.hyperreal" %% "b-tree" % "0.5", //local build

        "org.wvlet.airframe" %% "airframe-ulid" % "24.7.1",
        "ru.odnoklassniki" % "one-nio" % "1.7.3",

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
        "com.typesafe.akka"             %% "akka-discovery"                     % akkaVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap"  % AkkaMngVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http"       % AkkaMngVersion,

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
      "com.typesafe.akka" %% "akka-http-spray-json"        % AkkaHttpVersion,
      "com.typesafe.akka"             %% "akka-discovery"                     % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap"  % AkkaMngVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http"       % AkkaMngVersion,
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

    Assets / WebKeys.packagePrefix := "public/",
    (Runtime / managedClasspath) += (Assets / packageBin).value,
    assembly / mainClass := Some("linguistic.Application"),
    assembly / assemblyJarName := s"linguistic-$version.jar",
    // Resolve duplicates for Sbt Assembly
    assembly / assemblyMergeStrategy := {
      case PathList(xs @ _*) if xs.last == "io.netty.versions.properties" =>
        MergeStrategy.rename
      case other                                                          =>
        (assembly / assemblyMergeStrategy).value(other)
    },
    docker / imageNames := Seq(ImageName(namespace = Some("haghard"), repository = "linguistic", tag = Some(version))),
    docker / buildOptions := BuildOptions(
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
      Process(s"mkdir $dir/target/web").!
      Process(s"mkdir $dir/target/web/web-modules").!
      Process(s"mkdir $dir/target/web/web-modules/main").!
      Process(s"mkdir $dir/target/web/web-modules/main/webjars").!
      Process(s"mkdir $dir/target/web/web-modules/main/webjars/lib").!
      Process(s"mkdir $dir/target/web/web-modules/main/webjars/lib/bootstrap").!
      Process(s"mkdir $dir/target/web/web-modules/main/webjars/lib/bootstrap/css").!
      Process(s"cp $dir/src/main/resources/main.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
      Process(s"cp $dir/src/main/resources/chat.css ${dir}/target/web/web-modules/main/webjars/lib/bootstrap/css/").!
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
    Test / scalaJSUseMainModuleInitializer := false,
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
    Compile / PB.targets := Seq(scalapb.gen(flatPackage = true) -> (Compile / sourceManaged).value / "protobuf")
  )

ThisBuild / scalafmtOnCompile := true

addCommandAlias("fmt", "scalafmt")
addCommandAlias("c", "compile")
addCommandAlias("r", "reload")


//onLoad in Global := (onLoad in Global).value andThen {s: State => "project server" :: s}