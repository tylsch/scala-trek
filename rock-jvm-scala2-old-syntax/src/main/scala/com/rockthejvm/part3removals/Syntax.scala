package com.rockthejvm.part3removals

object Syntax {
  // do-while instruction
  var i = 0
  do {
    println(i)
    i += 1
  } while (i < 10)

  // methods returning Unit
  def sayHi() {
    // some block returning Unit
    println("Weird function")
  }

  // limit 22 of arguments
//  val functionWithLotsOfArgs = (
//                                 x1: Int,
//                                 x2: Int,
//                                 x3: Int,
//                                 x4: Int,
//                                 x5: Int,
//                                 x6: Int,
//                                 x7: Int,
//                                 x8: Int,
//                                 x9: Int,
//                                 x10: Int,
//                                 x11: Int,
//                                 x12: Int,
//                                 x13: Int,
//                                 x14: Int,
//                                 x15: Int,
//                                 x16: Int,
//                                 x17: Int,
//                                 x18: Int,
//                                 x19: Int,
//                                 x20: Int,
//                                 x21: Int,
//                                 x22: Int,
//                                 x23: Int,
//                                 x24: Int,
//                                 x25: Int
//                               ) => println("lots of args")

  // methods with no arguments vs methods with an empty argument list
  def aParameterlessMethod = 42
  def aMethodWithEmptyArgList() = 42
  // in Scala 2, you can call both with the "parameterless" syntax
  val meaningOfLife = aParameterlessMethod //ok
  val meaningOfLife2 = aMethodWithEmptyArgList // ok, with warning
  // not the other way around
  //val meaningOfLife3 = aParameterlessMethod() // illegal

  // uninitialized vars
  var toAssignLater: Int = _ // ok
  // some time later
  toAssignLater = 87 // ok


  def main(args: Array[String]): Unit = {
    println(
      s"""
        |val functionWithLotsOfArgs = (
        | ${(1 to 25).map(i => s"x$i: Int").mkString(",\n")}
        |) => println("lots of args")
        |""".stripMargin)
  }
}
