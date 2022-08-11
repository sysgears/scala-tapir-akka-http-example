ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "tapir-example"
  )

val AkkaHttpVersion = "10.2.9"
val TapirVersion = "1.0.3"
val SttpVersion = "3.7.2"
val CirceVersion = "0.14.2"

libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc" % "4.2.0",
  "org.postgresql" % "postgresql" % "42.3.6",
  "com.softwaremill.macwire" %% "macros" % "2.5.7" % "provided",
  "io.jsonwebtoken" % "jjwt" % "0.9.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.scala-lang.modules" %% "scala-async" % "1.0.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % "1.0.4" % Test,
  "org.scalatestplus" %% "mockito-4-6" % "3.2.13.0" % Test,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % TapirVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceVersion,
  "io.circe" %% "circe-shapes" % CirceVersion,
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % SttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % SttpVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.apache.logging.log4j" % "log4j-core" % "2.18.0"
)
