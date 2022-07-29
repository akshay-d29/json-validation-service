import Dependencies.Testing._
import Dependencies.{Http4s, Circe, Logging}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "json-validation-service",
    resolvers ++= Seq(
      "jitpack".at("https://jitpack.io"),
      Resolver.defaultLocal
    ),
    libraryDependencies ++= {
      val compile = Seq(Logging.logger) ++ Http4s.all ++ Circe.all
      val test = Seq(scalaTest).map(_ % Test)

      compile ++ test
    }
  )
