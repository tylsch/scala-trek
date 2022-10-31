name := "rock-jvm-akka-http"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"
val akkaVersion = "2.7.0"
val akkaHttpVersion = "10.4.0"
val scalaTestVersion = "3.2.14"
val logbackVersion = "1.4.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  // logging
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  // testing
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion
)

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-akka-http"
  )
