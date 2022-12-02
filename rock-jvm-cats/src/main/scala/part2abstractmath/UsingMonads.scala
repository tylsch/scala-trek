package part2abstractmath

import jdk.internal.vm.vector.VectorSupport.VectorPayload

object UsingMonads {

  import cats.Monad
  import cats.instances.list._
  import cats.instances.option._
  val monadList = Monad[List]
  val aSimpleList: List[Int] = monadList.pure(2)
  val anExtendedList = monadList.flatMap(aSimpleList)(x => List(x, x + 1))
  // applicable to Option, Try, Future, ....

  // Either is also a Monad
  val aManualEither: Either[String, Int] = Right(42)
  type LoadingOr[T] = Either[String, T]
  type ErrorOr[T] = Either[Throwable, T]
  import cats.instances.either._
  val loadingMonad = Monad[LoadingOr]
  val anEither = loadingMonad.pure(45) // LoadingOr[Int] == Right(45)
  val aChangedLoading = loadingMonad.flatMap(anEither)(n => if (n % 2 == 0)  Right(n + 1) else Left("Loading meaning of life...."))

  // imaginary online store
  case class OrderStatus(orderId: Long, status: String)
  def getOrderStatus(orderId: Long): LoadingOr[OrderStatus] = Right(OrderStatus(orderId, "Ready to Ship"))
  def trackLocation(orderStatus: OrderStatus): LoadingOr[String] =
    if (orderStatus.orderId > 1000) Left("Not available yet, refreshing data...")
    else Right("Amsterdam, NL")

  val orderId = 457L
  val orderLocation = loadingMonad.flatMap(getOrderStatus(orderId))(trackLocation)
  // use extension methods
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  val orderLocationBetter: LoadingOr[String] = getOrderStatus(orderId).flatMap(trackLocation)
  val orderLocationFor: LoadingOr[String] = for {
    os <- getOrderStatus(orderId)
    l <- trackLocation(os)
  } yield l

  case class Connection(host: String, port: String)
  val config = Map(
    "host" -> "localhost",
    "port" -> "4040"
  )

  trait HttpService[M[_]] {
    def getConnection(cfg: Map[String, String]): M[Connection]
    def issueRequest(connection: Connection, payload: String): M[String]
  }

  def getResponse[M[_]](service: HttpService[M], payload: String)(implicit monad: Monad[M]): M[String] =
    for {
      c <- service.getConnection(config)
      r <- service.issueRequest(c, payload)
    } yield r

  object OptionHttpService extends HttpService[Option] {
    override def getConnection(cfg: Map[String, String]): Option[Connection] =
      for {
        h <- cfg.get("host")
        p <- cfg.get("port")
      } yield Connection(h, p)

    override def issueRequest(connection: Connection, payload: String): Option[String] =
      if (payload.length >= 20) None
      else Some(s"Request ($payload) has been accepted")
  }

  val responseOption: Option[String] = OptionHttpService.getConnection(config).flatMap(c => OptionHttpService.issueRequest(c, "Hello HTTP Service"))

  val responseOptionFor = for {
    c <- OptionHttpService.getConnection(config)
    r <- OptionHttpService.issueRequest(c, "Hello HTTP Service")
  } yield r

  object AggressiveHttpService extends HttpService[ErrorOr] {
    override def getConnection(cfg: Map[String, String]): ErrorOr[Connection] =
      if (!cfg.contains("host") || !cfg.contains("port")) Left(new RuntimeException("Connection could not be established"))
      else Right(Connection(cfg("host"), cfg("port")))
    override def issueRequest(connection: Connection, payload: String): ErrorOr[String] =
      if (payload.length >= 20) Left(new RuntimeException("Payload is too large"))
      else Right(s"Request ($payload) has been accepted")
  }

  val errorOrResponse = for {
    c <- AggressiveHttpService.getConnection(config)
    r <- AggressiveHttpService.issueRequest(c, "Hello ErrorOr")
  } yield r

  def main(args: Array[String]): Unit = {
    println(responseOption)
    println(errorOrResponse)
    println(getResponse(OptionHttpService, "Hello Option"))
    println(getResponse(AggressiveHttpService, "Hello ErrorOr"))
  }
}
