package com.rockthejvm.part3removals

object Syntax {

  // do-while instruction, Scala 3 does NOT support do-while anymore
//  var i = 0
//  do {
//    println(i)
//    i += 1
//  } while (i < 10)

  // Scala 3 requires =
//  def sayHi() {
//    // some block returning Unit
//    println("Weird function")
//  }

// limit 22 No problem in Scala 3
val functionWithLotsOfArgs = (
                               x1: Int,
                               x2: Int,
                               x3: Int,
                               x4: Int,
                               x5: Int,
                               x6: Int,
                               x7: Int,
                               x8: Int,
                               x9: Int,
                               x10: Int,
                               x11: Int,
                               x12: Int,
                               x13: Int,
                               x14: Int,
                               x15: Int,
                               x16: Int,
                               x17: Int,
                               x18: Int,
                               x19: Int,
                               x20: Int,
                               x21: Int,
                               x22: Int,
                               x23: Int,
                               x24: Int,
                               x25: Int
                             ) => println("lots of args")

  def aParameterlessMethod = 42

  def aMethodWithEmptyArgList() = 42

  // in Scala 2, you can call both with the "parameterless" syntax
  val meaningOfLife = aParameterlessMethod //ok
  //val meaningOfLife2 = aMethodWithEmptyArgList // not ok
  // not the other way around
  //val meaningOfLife3 = aParameterlessMethod() // illegal

  // uninitialized vars
  var toAssignLater: Int = _ // ok, but will be phased out in a future Scala 3 version
  // some time later
  toAssignLater = 87 // ok

  // Scala 3 style
  import scala.compiletime.uninitialized
  var toBeSetLater: Int = uninitialized
  //set later
  toBeSetLater = 64

  def main(args: Array[String]): Unit = {

  }
}
