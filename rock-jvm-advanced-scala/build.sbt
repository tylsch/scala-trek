ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-advanced-scala"
  )
