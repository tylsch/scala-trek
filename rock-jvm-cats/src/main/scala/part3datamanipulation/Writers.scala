package part3datamanipulation

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Writers {

  import cats.data.Writer
  // 1 - define them at the start
  val aWriter: Writer[List[String], Int] = Writer(List("Started something"), 45)
  // 2 - manipulate them with pure FP
  val anIncreasedWriter = aWriter.map(_ + 1) // value increases, logs stay the same
  val aLogsWriter = aWriter.mapWritten(_ :+ "found something interesting")
  val aWriterWithBoth = aWriter.bimap(_ :+ "found something interesting", _ + 1)
  val aWriterWithBoth2 = aWriter.mapBoth { (logs, value) =>
    (logs :+ "found something interesting", value + 1)
  }

  // flatMap
  import cats.instances.vector._ // imports a Semigroup[Vector]
  val writerA = Writer(Vector("Log A1", "Log A2"), 10)
  val writerB = Writer(Vector("Log B1", "Log B2"), 40)
  val compositeWriter = for {
    va <- writerA
    vb <- writerB
  } yield va + vb

  // reset the logs
  import cats.instances.list._ // an implicit Monoid[List[Int]]
  val anEmptyWriter = aWriter.reset


  // 3 - dump either the value or the logs
  val desiredValue = aWriter.value
  val logs = aWriter.written
  val (l, v) = aWriter.run

  def countAndSay(n: Int): Unit = {
    if (n <= 0) println("starting!")
    else {
      countAndSay(n - 1)
      println(n)
    }
  }

  def countAndLog(n: Int): Writer[Vector[String], Int] = {
    if (n <= 0) Writer(Vector("starting!"), 0)
    else countAndLog(n - 1).flatMap(_ => Writer(Vector(s"$n"), n))
  }

  def naiveSum(n: Int): Int = {
    if (n <= 0) 0
    else {
      println(s"Now at $n")
      val lowerSum = naiveSum(n - 1)
      println(s"Computed sum (${n - 1}) = $lowerSum")
      lowerSum + n
    }
  }

  def sumWithLogs(n: Int): Writer[Vector[String], Int] = {
    if (n <= 0) Writer(Vector(), 0)
    else for {
      _ <- Writer(Vector(s"Now at ${n}"), n)
      lowerSum <- sumWithLogs(n - 1)
      _ <- Writer(Vector(s"Computed sum (${n - 1}) = $lowerSum"), n)
    } yield lowerSum + n
  }

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(9))
  def main(args: Array[String]): Unit = {
    println(compositeWriter.run)

    countAndSay(10)
    countAndLog(10).written.foreach(println)

    Future(naiveSum(100)).foreach(println)
    Future(naiveSum(100)).foreach(println)

    val sumFuture1 = Future(sumWithLogs(100))
    val sumFuture2 = Future(sumWithLogs(100))
    val logs1 = sumFuture1.map(_.written)
    val logs = sumFuture2.map(_.written)

    //sumWithLogs(100).written.foreach(println)
  }
}
