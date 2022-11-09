package com.rockthejvm

object Main extends App {
  val fansiStr: fansi.Str = fansi.Color.Red("This should be a red string")
  println(fansiStr)
}
