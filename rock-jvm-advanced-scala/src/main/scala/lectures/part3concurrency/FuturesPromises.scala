package lectures.part3concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}

object FuturesPromises extends App {
  def calculateMeaningOfLife: Int = {
    Thread.sleep(2000)
    42
  }

  val aFuture = Future {
    calculateMeaningOfLife
  }

  // println(aFuture.value) // Option[Try[Int]]
  println("Waiting on the future...")
  aFuture.onComplete {
    case Success(value) => println(s"The meaning of life is $value")
    case Failure(exception) => println(s"I have failed with $exception")
  } // SOME thread

  Thread.sleep(3000)

  // Mini social network
  case class Profile(id: String, name: String) {
    def poke(anotherProfile: Profile): Unit =
      println(s"${this.name} poking ${anotherProfile.name}")
  }
  object SocialNetwork {
    // "database" of profiles
    val names = Map(
      "fb.id.1-zuck" -> "Mark",
      "fb.id.2-bill" -> "Bill",
      "fb.id.0-dummy" -> "Dummy"
    )
    val friends = Map(
      "fb.id.1-zuck" -> "fb.id.2-bill"
    )

    val random = new Random()
    // API
    def fetchProfile(id: String): Future[Profile] = Future {
      Thread.sleep(random.nextInt(300))
      Profile(id = id, name = names(id))
    }

    def fetchBestFriend(profile: Profile): Future[Profile] = Future {
      Thread.sleep(random.nextInt(400))
      val bfId = friends(profile.id)
      Profile(bfId, names(bfId))
    }
  }

  // Client: Mark to poke Bill
  val mark = SocialNetwork.fetchProfile("fb.id.1-zuck")
  //    mark.onComplete {
  //      case Success(markProfile) => {
  //        val bill = SocialNetwork.fetchBestFriend(markProfile)
  //        bill.onComplete {
  //          case Success(billProfile) => markProfile.poke(billProfile)
  //          case Failure(exception) => exception.printStackTrace()
  //        }
  //      }
  //      case Failure(exception) => exception.printStackTrace()
  //    }

  val nameOnTheWall: Future[String] = mark.map(profile => profile.name)
  val marksBestFriend: Future[Profile] = mark.flatMap(profile => SocialNetwork.fetchBestFriend(profile))
  val zucksBestFriendRestricted: Future[Profile] = marksBestFriend.filter(profile => profile.name.startsWith("Z"))

  for {
    mark <- SocialNetwork.fetchProfile("fb.id.1-zuck")
    bill <- SocialNetwork.fetchBestFriend(mark)
  } mark.poke(bill)

  Thread.sleep(2000)

  // Fallbacks
  val aProfileNoMatterWhat = SocialNetwork.fetchProfile("unknown-id").recover {
    case e: Throwable => Profile("fb.id.0-dummy", "Forever Alone")
  }

  val aFetchedProfileNoMatterWhat = SocialNetwork.fetchProfile("unknown").recoverWith {
    case e: Throwable => SocialNetwork.fetchProfile("fb.id.0-dummy")
  }

  val fallbackResult = SocialNetwork.fetchProfile("unknown").fallbackTo(SocialNetwork.fetchProfile("fb.id.0-dummy"))

  // online banking app
  case class User(name: String)

  case class Transaction(sender: String, receiver: String, amount: Double, status: String)

  object BankingApp {
    val name = "Rock the JVM Banking"

    def fetchUser(name: String): Future[User] = Future {
      Thread.sleep(500)
      User(name)
    }

    def createTransaction(user: User, merchantName: String, amount: Double): Future[Transaction] = Future {
      Thread.sleep(1000)
      Transaction(user.name, merchantName, amount, "Success")
    }

    def purchase(username: String, item: String, merchantName: String, cost: Double): String = {
      // fetch user from the DB
      // create transaction
      // WAIT for the transaction to finish

      val status = for {
        user <- fetchUser(username)
        transaction <- createTransaction(user, merchantName, cost)
      } yield transaction.status

      Await.result(status, 2 seconds) // implicit conversions -> pimp my library
    }
  }

  println(BankingApp.purchase("Daniel", "iPhone 14", "Rock the JVM Store", 3000))

  // promises
  val promise = Promise[Int]() // "controller" over a future
  val future = promise.future

  // Thread 1 - "consumer"
  future.onComplete {
    case Success(value) => println(s"[consumer] I've received $value")
  }

  val producer = new Thread(() => {
    println("[producer] crunching numbers...")
    Thread.sleep(1000)
    promise.success(42)
    println("[producer] done")
  })

  producer.start()
  Thread.sleep(1000)

  /*
  * 1) FulFill a future IMMEDIATELY with a value
  * 2) inSequence(fa, fb)
  * 3) first(fa, fb) => new Future with the first value of the two futures
  * 4) last(fa, fb) => new Future with the last value of the two futures
  * 5) retryUntil(action: () => Future[T], condition: T => Boolean): Future[T]
  * */

  // 1)
  def fulfillImmediately[T](value: T): Future[T] = Future(value)

  // 2)
  def inSequence[A, B](first: Future[A], second: Future[B]): Future[B] =
    first.flatMap(_ => second)

  // 3)
  def first[A](fa: Future[A], fb: Future[A]): Future[A] = {
    val promise = Promise[A]
    def tryComplete(promise: Promise[A], result: Try[A]) = result match {
      case Success(value) => try {
        promise.success(value)
      } catch {
        case _ =>
      }
      case Failure(exception) => try {
        promise.failure(exception)
      } catch {
        case _ =>
      }
    }
    fa.onComplete(promise.tryComplete)
    fb.onComplete(promise.tryComplete)

    promise.future
  }

  // 4)
  def last[A](fa: Future[A], fb: Future[A]): Future[A] = {
    val promise = Promise[A]
    val lastPromise = Promise[A]
    val checkAndComplete = (result: Try[A]) =>
      if (!promise.tryComplete(result))
        lastPromise.complete(result)

    fa.onComplete(checkAndComplete)
    fb.onComplete(checkAndComplete)

    lastPromise.future
  }

  val fast = Future {
    Thread.sleep(100)
    42
  }
  val slow = Future {
    Thread.sleep(200)
    45
  }
  first(fast, slow).foreach(println)
  last(fast, slow).foreach(println)
  Thread.sleep(1000)

  // 5)
  def retryUntil[T](action: () => Future[T], condition: T => Boolean): Future[T] =
    action()
      .filter(condition)
      .recoverWith {
        case _ => retryUntil(action, condition)
      }

  val random = new Random()
  val action = () => Future {
    Thread.sleep(100)
    val nextValue = random.nextInt(100)
    println(s"Generated $nextValue")
    nextValue
  }

  retryUntil(action, (x: Int) => x < 50).foreach(result => println(s"settle at $result"))
  Thread.sleep(10000)
}
