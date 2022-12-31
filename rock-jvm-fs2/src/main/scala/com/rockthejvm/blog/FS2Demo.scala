package com.rockthejvm.blog

import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.{Chunk, Pipe, Pull, Pure, Stream}

object FS2Demo extends IOApp.Simple {
  case class Actor(id: Int, firstName: String, lastName: String)

  object Data:
    // Justice League
    val henryCavil: Actor = Actor(0, "Henry", "Cavill")
    val galGodot: Actor = Actor(1, "Gal", "Godot")
    val ezraMiller: Actor = Actor(2, "Ezra", "Miller")
    val benFisher: Actor = Actor(3, "Ben", "Fisher")
    val rayHardy: Actor = Actor(4, "Ray", "Hardy")
    val jasonMomoa: Actor = Actor(5, "Jason", "Momoa")

    // Avengers
    val scarlettJohansson: Actor = Actor(6, "Scarlett", "Johansson")
    val robertDowneyJr: Actor = Actor(7, "Robert", "Downey Jr.")
    val chrisEvans: Actor = Actor(8, "Chris", "Evans")
    val markRuffalo: Actor = Actor(9, "Mark", "Ruffalo")
    val chrisHemsworth: Actor = Actor(10, "Chris", "Hemsworth")
    val jeremyRenner: Actor = Actor(11, "Jeremy", "Renner")
    val tomHolland: Actor = Actor(13, "Tom", "Holland")
    val tobeyMaguire: Actor = Actor(14, "Tobey", "Maguire")
    val andrewGarfield: Actor = Actor(15, "Andrew", "Garfield")

  // streams = abstraction to manage an unbounded amount of data
  // IO = any kind of computation that might perform side effects
  // pure streams = store actual data
  import Data.*
  import com.rockthejvm.blog.utils.*
  val jlActors: Stream[Pure, Actor] = Stream(henryCavil, galGodot, ezraMiller, benFisher, rayHardy, jasonMomoa)

  val tomHollandStream: Stream[Pure, Actor] = Stream.emit(tomHolland)
  val spiderMen = Stream.emits(List(tomHolland, andrewGarfield, tobeyMaguire))

  // convert a Stream to a standard data structure
  val jlActorsList = jlActors.toList // applicable for Stream[Pure, _]

  // infinite streams
  val infiniteJlActors = jlActors.repeat
  val repeatedJlActorsList = infiniteJlActors.take(10).toList

  // effectful streams
  val savingTomHolland: Stream[IO, Actor] = Stream.eval {
    IO {
      println("Saving actor Tom Holland into the DB")
      Thread.sleep(1000)
      tomHolland
    }
  }

  // COMPILE
  val compiledStream: IO[Unit] = savingTomHolland.compile.drain

  // chunks
  val avengersActors: Stream[Pure, Actor] = Stream.chunk(Chunk.array(Array(scarlettJohansson, robertDowneyJr, chrisEvans, markRuffalo, chrisHemsworth, jeremyRenner, tomHolland, andrewGarfield, tobeyMaguire)))

  // transformations
  val allSuperheros = jlActors ++ avengersActors

  // flatMap
  val printedJlActors: Stream[IO, Unit] = jlActors.flatMap { actor =>
    // perform IO[Unit] effect as a Stream
    Stream.eval(IO.println(actor))
  }

  // flatMap + eval = evalMap
  val printedJlActors_v2: Stream[IO, Unit] = jlActors.evalMap(IO.println)
  // flatMap + eval while keeping the original type = evalTap
  val printedJlActors_v3: Stream[IO, Actor] = jlActors.evalTap(IO.println)

  override def run: IO[Unit] = compiledStream
}
