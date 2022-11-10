val scala212 = "2.12.16"
val scala213 = "2.13.10"

ThisBuild / scalaVersion := scala213
ThisBuild / version := "1.0"
ThisBuild / name := "rock-jvm-multi-module"
ThisBuild / organization := "com.rockthejvm"

// add external resolver
// resolvers += Resolver.url("my-test-repo", "https://rockthejvm.com/repository/...")

// add Maven local repo
// resolver += Resolver.mavenLocal

// custom task
lazy val printerTask = taskKey[Unit]("Custom Printer Task")
printerTask := {
  val uuidTask = uuidStringTask.value
  println(s"Generated uuid: $uuidTask")

  val uuidSetting = uuidStringSetting.value
  println(s"Generated uuid from setting: $uuidSetting")
  CustomTaskPrinter.print()
}

lazy val uuidStringTask = taskKey[String]("Random UUID generator")
uuidStringTask := {
  StringTask.strTask()
}

// custom settings
lazy val uuidStringSetting = settingKey[String]("Random UUID Setting")
uuidStringSetting := {
  val uuid = StringTask.strTask()
  uuid
}

// command aliases
addCommandAlias("ci", "compile;test;assembly")

lazy val core = (project in file("core")).settings(
  assembly / mainClass := Some("com.rockthejvm.CoreApp"),
  libraryDependencies += Constants.rootPackage %% "cats-effect" % "3.3.1",
  crossScalaVersions := List(scala212, scala213)
)
lazy val server = (project in file("server")).dependsOn(core)

lazy val root = (project in file(".")).aggregate(core, server)
