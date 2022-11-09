ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "1.0"
ThisBuild / name := "rock-jvm-multi-module"
ThisBuild / organization := "com.rockthejvm"

lazy val core = (project in file("core")).settings(
  assembly / mainClass := Some("com.rockthejvm.CoreApp"),
  libraryDependencies += Constants.rootPackage %% "cats-effect" % "3.3.1"
)
lazy val server = (project in file("server")).dependsOn(core)

lazy val root = (project in file(".")).aggregate(core, server)
