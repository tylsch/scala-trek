package com.rockthejvm.blog

import zio.*
import zio.stream.*

object ZIOStreams extends ZIOAppDefault:
  override def run: ZIO[Any, Any, Any] = ZIO.succeed(42)

