package com.rockthejvm.blog.prep

import zio.*

object ZIOHTTP extends ZIOAppDefault:
  override def run: ZIO[Any, Any, Any] = ZIO.succeed(42)
