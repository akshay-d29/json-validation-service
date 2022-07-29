import sbt._

object Dependencies {

  object Logging {
    val logger ="ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
  }

  object Circe {
    val schema = "io.circe" %% "circe-json-schema" % "0.1.0"

    private val circeVersion = "0.14.1"
    
    def all: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion) :+ schema
  }

  object Http4s {
    private val http4sVersion = "0.23.12"

    def all: Seq[ModuleID] = Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion
    )
  }

  object Testing {
    val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12"
  }
}
