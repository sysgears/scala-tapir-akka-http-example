ThisBuild / version := "1.0.1"

ThisBuild / scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .settings(
    name := "tapir-example"
  )

val AkkaHttpVersion = "10.2.9"
val TapirVersion = "1.13.3"
val SttpVersion = "3.11.0"
val CirceVersion = "0.14.15"

libraryDependencies ++= Seq(
  "io.getquill" %% "quill-jdbc-zio" % "4.8.5",
  "org.postgresql" % "postgresql" % "42.7.8",
  "dev.zio" %% "zio" % "2.1.23",
  "dev.zio" %% "zio-test" % "2.1.23",
  "dev.zio" %% "zio-interop-cats" % "23.1.0.13",
  "io.jsonwebtoken" % "jjwt" % "0.13.0",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % "1.13.3" % Test,
  "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test,
  "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % TapirVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-shapes" % CirceVersion,
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % SttpVersion,
  "com.softwaremill.sttp.client3" %% "circe" % SttpVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.23",
  "org.apache.logging.log4j" % "log4j-core" % "2.25.3"
)
