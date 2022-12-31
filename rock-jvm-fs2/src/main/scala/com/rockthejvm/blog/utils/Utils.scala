package com.rockthejvm.blog.utils

import cats.effect.IO
extension [A](io: IO[A])
  def debugM: IO[A] = for {
    a <- io
    t = Thread.currentThread().getName
    _ = println(s"[$t] $a")
  } yield a