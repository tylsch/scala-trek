package lectures.part3concurrency

import java.util.concurrent.Executors

object Intro extends App {
  /*
  * Interface Runnable {
  *   public void run()
  * }
  * */
  // JVM Threads
  val runnable = new Runnable {
    override def run(): Unit = println("Running in parallel")
  }
  val aThread = new Thread(runnable)

//  aThread.start() // Create a JVM Thread => OS Thread, gives signal to JVM to start JVM Thread
//  runnable.run() // doesn't do anything
//  aThread.join() // blocks until aThread finishes running

  val threadHello = new Thread(() => (1 to 5).foreach(_ => println("hello")))
  val threadGoodbye = new Thread(() => (1 to 5).foreach(_ => println("goodbye")))
//  threadHello.start()
//  threadGoodbye.start()
  // different runs produce different results!

  // executors
  val pool = Executors.newFixedThreadPool(10)
//  pool.execute(() => println("something in the thread pool"))
//
//  pool.execute(() => {
//    Thread.sleep(1000)
//    println("done after one second")
//  })
//
//  pool.execute(() => {
//    Thread.sleep(1000)
//    println("Almost done")
//    Thread.sleep(1000)
//    println("done after 2 seconds")
//  })

  pool.shutdown()
//  pool.execute(() => println("should not appear")) // throws an exception in the calling thread
//  pool.shutdownNow()
  println(pool.isShutdown) // true

  // race condition
  def runInParallel = {
    var x = 0

    val thread1 = new Thread(() => {
      x = 1
    })

    val thread2 = new Thread(() => {
      x = 2
    })

    thread1.start()
    thread2.start()
    println(x)
  }

  //for (_ <- 1 to 10000) runInParallel

  class BankAccount(@volatile var amount: Int) {
    override def toString: String = "" + amount
  }

  def buy(account: BankAccount, thing: String, price: Int): Unit = {
    account.amount -= price
//    println("I've bought " + thing)
//    println("my account is now " + account)
  }

//  for (_ <- 1 to 10000) {
//    val account = new BankAccount(50000)
//    val thread1 = new Thread(() => buy(account, "shoes", 3000))
//    val thread2 = new Thread(() => buy(account, "iPhone 12", 4000))
//
//    thread1.start()
//    thread2.start()
//    Thread.sleep(10)
//    if (account.amount != 43000) println("AHA: " + account.amount)
//  }

  // #1 use synchronized()
  def buySafe(account: BankAccount, thing: String, price: Int): Unit = {
    account.synchronized {
      account.amount -= price
      println("I've bought " + thing)
      println("my account is now " + account)
    }
  }

  // #2 use @volatile on variable

  /*
  * Exercises
  * 1) Construct 50 "inception" threads
  *   Thread1 -> thread2 -> thread3 -> ....
  *   - print greetings in REVERSE order
  *
  * 2)
  * */

  def inceptionThreads(maxThreads: Int, index: Int = 1): Thread = new Thread(() => {
    if (index < maxThreads) {
      val newThread = inceptionThreads(maxThreads, index + 1)
      newThread.start()
      newThread.join()
    }
    println(s"Hello from thread $index")
  })

  inceptionThreads(50).start()

  var x = 0
  val threads = (1 to 100).map(_ => new Thread(() => x += 1))
  threads.foreach(_.start())
  // 1) What is the biggest possible value for x?
  // 2) What is the smallest possible values for x?
  threads.foreach(_.join())
  println(x)

  // 3) Sleep Fallacy
  var message = ""
  val awesomeThread = new Thread(() => {
    Thread.sleep(1000)
    message = "Scala is awesome"
  })

  message = "Scala sucks"
  awesomeThread.start()
  Thread.sleep(2000)
  println(message) // What is the value of message?  Always "Scala is awesome" but not guaranteed due to sleep fallacy
}
