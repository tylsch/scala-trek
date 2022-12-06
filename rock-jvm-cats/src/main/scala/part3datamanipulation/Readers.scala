package part3datamanipulation

import cats.Id
import cats.data.Kleisli

object Readers {

  /*
  * Configuration File => initial data structure
  * - DB Layer
  * - HTTP Layer
  * - Business Logic Layer
  * */

  case class Configuration(userName: String, password: String, host: String, port: Int, nThreads: Int, emailReplyTo: String)
  case class DbConnection(username: String, password: String) {
    def getOrderStatus(orderId: Long): String = "dispatcher"
    def getLastOrderId(username: String): Long = 542643
  }
  case class HttpService(host: String, port: Int) {
    def start(): Unit = println("Server Started")
  }

  val config = Configuration("admin", "admin", "localhost", 80, 2, "daniel@rockthejvm.com")
  // cats reader
  import cats.data.Reader
  val dbReader: Reader[Configuration, DbConnection] = Reader(conf => DbConnection(conf.userName, conf.password))
  val dbConn = dbReader.run(config)

  //Reader[I, O]
  val danielsOrderStatusReader: Reader[Configuration, String] = dbReader.map(dbConn => dbConn.getOrderStatus(55))
  val danielsOrderStatus: String = danielsOrderStatusReader.run(config)

  def getLastOrderStatus(username: String): String = {
    val usersLastOrderIdReader: Reader[Configuration, String] = dbReader
      .map(_.getLastOrderId(username))
      .flatMap(lastOrderId => dbReader.map(_.getOrderStatus(lastOrderId)))

    val usersLastOrderIdForReader = for {
      lastOrderId <- dbReader.map(_.getLastOrderId(username))
      orderStatus <- dbReader.map(_.getOrderStatus(lastOrderId))
    } yield orderStatus

    usersLastOrderIdForReader.run(config)
  }

  case class EmailService(emailReplyTo: String) {
    def sendEmail(address: String, contents: String) = s"Sending email to $address: $contents"
  }

  def emailUser(username: String, userEmail: String): String = {
    val emailServiceReader: Reader[Configuration, EmailService] = Reader(conf => EmailService(conf.emailReplyTo))
    val emailReader: Reader[Configuration, String] = for {
      lastOrderId <- dbReader.map(_.getLastOrderId(username))
      orderStatus <- dbReader.map(_.getOrderStatus(lastOrderId))
      emailService <- emailServiceReader
    } yield emailService.sendEmail(userEmail, s"Your last order as the status: $orderStatus")

    emailReader.run(config)
  }

  def main(args: Array[String]): Unit = {
    println(getLastOrderStatus("daniel"))
    println(emailUser("daniel", "johndoe@rockthejvm.com"))
  }
}
