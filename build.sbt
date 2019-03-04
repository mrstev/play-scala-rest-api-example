import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

scalaVersion in ThisBuild := "2.12.7"

// dependency injection done by google
libraryDependencies += guice

// libs for supporting db evolutions
libraryDependencies ++= Seq(evolutions, jdbc)
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.38"


// dependency injection lib to help with google's guice
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.1"

// lib for date & time
libraryDependencies += "org.joda" % "joda-convert" % "2.1.2"

// lib for 
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "5.2"

// routing lib
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"

// testing lib
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test

// load testing libs 
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.1.1" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "3.0.1.1" % Test

// lib for Scala DSL to 
libraryDependencies += "io.getquill" %% "quill-async-mysql" % "3.0.1"


// lib for using sealed trait to write variant json
libraryDependencies += "org.julienrf" %% "play-json-derived-codecs" % "5.0.0"


// libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.0"
// libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "4.0.0"

// libraryDependencies += "com.h2database" % "h2" % "1.4.197"

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """bill-of-materials-rest-api""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("module-info.class") =>
    MergeStrategy.concat
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}