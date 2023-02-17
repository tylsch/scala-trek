ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-zio-http",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.9",
      "dev.zio" %% "zio-streams" % "2.0.9",
      "dev.zio" %% "zio-json" % "0.4.2",
      "io.d11" %% "zhttp"      % "2.0.0-RC7"
    )
  )
