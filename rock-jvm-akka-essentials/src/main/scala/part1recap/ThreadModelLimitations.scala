package part1recap

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object ThreadModelLimitations {

  // Daniel's Rants
  /*
  * DR #1: Object Oriented encapsulation is only valid in the SINGLE-THREADED MODEL
  * DR #2: Delegating a task to a thread
  * DR #3: Tracing and dealing with errors is a PAIN
  * */

  class BankAccount(private var amount: Int) {
    override def toString: String = s"$amount"

    def withdraw(money: Int) = synchronized {
      this.amount -= money
    }
    def deposit(money: Int) = synchronized{
      this.amount += money
    }
    def getAmount = amount
  }
  val account = new BankAccount(2000)
  val depositThreads = (1 to 1000).map(_ => new Thread(() => account.deposit(1)))
  val withdrawThreads = (1 to 1000).map(_ => new Thread(() => account.withdraw(1)))

  /*
  * We don't know when the threads are finished
  * race conditions
  *
  * solution: synchronization
  * other problems:
  * - dead locks
  * - live locks
  * */

  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] I have a task!")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if (task == null) {
      task = r
      runningThread.synchronized {
        runningThread.notify()
      }
    }
  }

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  // sum 1m numbers in between 10 threads

  val futures = (0 to 9)
    .map(i => BigInt(100000 * i) until BigInt(100000 * (i + 1)))
    .map(range => Future {
      // bug
      if (range.contains(BigInt(546732))) throw new RuntimeException("invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _)

  def main(args: Array[String]): Unit = {
    sumFuture.onComplete(println)
  }
}
