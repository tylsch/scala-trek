package lectures.part4implicits

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MagnetPattern extends App {
  // method overloading
  class P2PRequest
  class P2PResponse
  class Serializer[T]

  trait Actor {
    def receive(statusCode: Int): Int
    def receive(request: P2PRequest): Int
    def receive(response: P2PResponse): Int
    def receive[T : Serializer](message: T): Int
    def receive[T : Serializer](message: T, statusCode: Int): Int
    def receive(future: Future[P2PRequest]): Int
    // lots of overloads
  }

  /*
  * 1) Type Erasure
  * 2) lifting doesn't work for all overloads
  * 3) code duplication
  * 4) type inference and default args
  * */

  trait MessageMagnet[Result] {
    def apply(): Result
  }

  def receive[R](magnet: MessageMagnet[R]): R = magnet()

  implicit class FromP2PRequest(request: P2PRequest) extends MessageMagnet[Int] {
    override def apply(): Int = {
      println("Handling P2PRequest request")
      42
    }
  }
  implicit class FromP2PResponse(response: P2PResponse) extends MessageMagnet[Int] {
    override def apply(): Int = {
      println("Handling P2PResponse response")
      24
    }
  }

  receive(new P2PRequest)
  receive(new P2PResponse)

  // 1 - no more type erasure problems
  implicit class FromResponse(future: Future[P2PResponse]) extends MessageMagnet[Int] {
    override def apply(): Int = 2
  }
  implicit class FromRequest(future: Future[P2PRequest]) extends  MessageMagnet[Int] {
    override def apply(): Int = 3
  }

  println(receive(Future(new P2PRequest)))
  println(receive(Future(new P2PResponse)))

  // 2 - lifting works
  trait MathLib {
    def add1(x: Int) = x + 1
    def add1(string: String) = string.toInt + 1
  }
  trait AddMagnet {
    def apply(): Int
  }

  def add1(magnet: AddMagnet): Int = magnet()

  implicit class AddInt(x: Int) extends AddMagnet {
    override def apply(): Int = x + 1
  }
  implicit class AddString(s: String) extends AddMagnet {
    override def apply(): Int = s.toInt + 1
  }

  val addFV = add1 _
  println(addFV(1))
  println(addFV("3"))

  val receiveFV = receive _
//  receiveFV(new P2PResponse)

  /*
  * Drawbacks
  * 1) verbose
  * 2) harder to read
  * 3) you can't name or place default args
  * 4) call by name doesn't work correctly
  * */

  class Handler {
    def handle(string: => String) = {
      println(string)
      println(string)
    }
  }

  trait HandleMagnet {
    def apply(): Unit
  }

  def handle(magnet: HandleMagnet) = magnet()

  implicit class StringHandle(string: => String) extends HandleMagnet {
    override def apply(): Unit = {
      println(string)
      println(string)
    }
  }

  def sideEffectMethod(): String = {
    println("Hello, Scala")
    "hahaha"
  }

//  handle(sideEffectMethod())
  handle {
    println("Hello, Scala")
    "magnet"
  } // careful!!!
}
