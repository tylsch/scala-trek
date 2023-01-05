ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "rock-jvm-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.5",
      "dev.zio" %% "zio-test" % "2.0.5",
      "dev.zio" %% "zio-test-sbt" % "2.0.5",
      "dev.zio" %% "zio-streams" % "2.0.5",
      "dev.zio" %% "zio-test-junit" % "2.0.5"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
