package lectures.part3concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

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
}
