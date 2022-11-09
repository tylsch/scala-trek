ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "fansi" % "0.4.0",
  "org.scalatest" %% "scalatest" % "3.2.14" % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-sbt",
    organization := "com.rockthejvm"
  )
