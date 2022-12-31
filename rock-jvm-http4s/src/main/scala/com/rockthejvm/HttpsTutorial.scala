package com.rockthejvm

import cats.*
import cats.effect.*
import cats.implicits.*
import org.http4s.circe.*
import org.http4s.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.server.*

object HttpsTutorial extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = IO(println("Hello, http4s")).as(ExitCode.Success)
}
