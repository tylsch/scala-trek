package com.rockthejvm.blog

import zio.*
import zio.stream.*
import zio.json.*

object ZIOStreams extends ZIOAppDefault:

  // effects
  val aSuccess: ZIO[Any, Nothing, Int] = ZIO.succeed(42)

  // ZStream = "collection" of 0 or more (maybe infinite) elements
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

  override def run: ZIO[Any, Any, Any] = zio_v3.debug

