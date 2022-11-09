package com.rockthejvm
import org.scalatest.funsuite.AnyFunSuite

class SimpleTestScalaSpec extends AnyFunSuite {
  test("simplest test possible") {
    assert("Scala".toLowerCase == "scala")
  }
}
