ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "tapir-example"
  )

val AkkaHttpVersion = "10.2.9"
val TapirVersion = "1.0.1"
val SttpVersion = "3.7.1"
val CirceVersion = "0.14.2"

libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc" % "3.18.0",
  "org.postgresql" % "postgresql" % "42.3.6",
  "org.scala-lang.modules" %% "scala-async" % "1.0.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % TapirVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceVersion,
  "io.circe" %% "circe-shapes" % CirceVersion,
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % SttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % SttpVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.apache.logging.log4j" % "log4j-core" % "2.17.2"
)
