ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % "1.0.0-M37",
      "org.http4s" %% "http4s-circe" % "1.0.0-M37",
      "org.http4s" %% "http4s-dsl" % "1.0.0-M37",
      "io.circe" %% "circe-generic" % "0.14.3",
    )
  )
