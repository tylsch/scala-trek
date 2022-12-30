ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-fs2",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.4.0"
    )
  )
