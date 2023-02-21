package com.rockthejvm.blog

import zio.*
import zio.stream.*
import zio.json.*

import java.io.{IOException, InputStream}

object ZIOStreams extends ZIOAppDefault:

  // effects
  val aSuccess: ZIO[Any, Nothing, Int] = ZIO.succeed(42)

  // ZStream = "collection" (source) of 0 or more (maybe infinite) elements
  val aStream: ZStream[Any, Nothing, Int] = ZStream.fromIterable(1 to 10)
  val intStream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3, 4, 5, 6, 7, 8)
  val stringStream: ZStream[Any, Nothing, String] = intStream.map(_.toString)

  // sink = destination of your elements
  val sum: ZSink[Any, Nothing, Int, Nothing, Int] =
    ZSink.sum[Int]

  val take5: ZSink[Any, Nothing, Int, Int, Chunk[Int]] =
    ZSink.take(5)

  val take5Map: ZSink[Any, Nothing, Int, Int, Chunk[String]] =
    take5.map(chunk => chunk.map(_.toString))

  // leftovers
  val take5Leftovers: ZSink[Any, Nothing, Int, Nothing, (Chunk[String], Chunk[Int])] =
    take5Map.collectLeftover                             // ^^ output   ^^ leftovers

  val take5Ignore: ZSink[Any, Nothing, Int, Int, Chunk[Int]] =
    take5.ignoreLeftover

  // contramap
  val take5String: ZSink[Any, Nothing, String, Int, Chunk[Int]] =
    take5.contramap(_.toInt)

  // ZStream[String] -> ZSink[Int].contramap(...)
  // same as
  // ZStream[String].map(...) -> ZSink[Int]

  val zioStreamEffect: ZIO[Any, Nothing, RuntimeFlags] = intStream.run(sum)

  // ZPipeline
  val businessLogic: ZPipeline[Any, Nothing, String, Int] =
    ZPipeline.map(_.toInt)

  val zio_v2: ZIO[Any, Nothing, RuntimeFlags] = stringStream.via(businessLogic).run(sum)

  // many pipelines
  val filterLogic: ZPipeline[Any, Nothing, Int, Int] =
    ZPipeline.filter(_ % 2 == 0)

  val appLogic: ZPipeline[Any, Nothing, String, Int] =
    businessLogic >>> filterLogic

  val zio_v3 = stringStream.via(appLogic).run(sum)

  val failStream: ZStream[Any, String, Int] = ZStream(1, 2) ++ ZStream.fail("Something bad") ++ ZStream(4,5)

  class FakeInputStream[T <: Throwable](limit: Int, failAt: Int, failWith: => T) extends InputStream:
    val data: Array[Byte] = "0123456789".getBytes
    var counter: Int = 0
    var index: Int = 0

    override def read(): RuntimeFlags =
      if (counter == limit) -1
      else if (counter == failAt) throw failWith
      else
        val result = data(index)
        index = (index + 1) % data.length
        counter += 1
        result

  val nonFailingStream: ZStream[Any, IOException, String] =
    ZStream.fromInputStream(new FakeInputStream(12, 99, new IOException("Something bad")), chunkSize = 1)
      .map(byte => new String(Array(byte)))

  val sink: ZSink[Any, Nothing, String, Nothing, String] =
    ZSink.collectAll[String].map(chunk => chunk.mkString("-"))

  val failingStream: ZStream[Any, IOException, String] =
    ZStream.fromInputStream(new FakeInputStream(10, 5, new IOException("Something bad")), chunkSize = 1)
      .map(byte => new String(Array(byte)))

  val defectStream: ZStream[Any, IOException, String] =
    ZStream.fromInputStream(new FakeInputStream(10, 5, new RuntimeException("Something unforeseen")), chunkSize = 1)
      .map(byte => new String(Array(byte)))

  // recovery
  val recoveryStream: ZStream[Any, Throwable, String] =
    ZStream("a", "b", "c")

  // orElse - chain a new stream AT THE POINT of failure
  val recoveredEffect = failingStream.orElse(recoveryStream).run(sink)
  // orElseEither - all elements from the original stream = Left, all elements from the fallback = Right
  val recoveredWithEither: ZStream[Any, Throwable, Either[String, String]] = failingStream.orElseEither(recoveryStream)


  // catch
  val caughtErrors = failingStream.catchSome {
    case _: IOException => recoveryStream
  }

  // catchSomeCause, catchAll, catchAllCause
  val errorContained: ZStream[Any, Nothing, Either[IOException, String]] = failingStream.either


  override def run: ZIO[Any, Any, Any] = errorContained.run(ZSink.collectAll).debug

