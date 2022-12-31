ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-doobie",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC1",
      "io.estatico" %% "newtype" % "0.4.4"
    )
  )
