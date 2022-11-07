package part3_highlevelserver

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class Book(id: Int, author: String, title: String)

class RouteDSLSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  import RouteDSLSpec._

  "A digital library backend" should {
    "return all the books in the library" in {
      Get("/api/book") ~> libraryRoute ~> check {
        // assertions
        status shouldBe StatusCodes.OK

        entityAs[List[Book]] shouldBe books
      }
    }
    "return a book by hitting the query parameter endpoint" in {
      Get("/api/book?id=2") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Option[Book]] shouldBe Some(Book(2, "JRR Tolkien", "The Lord of the Rings"))
      }
    }
    "return a book by calling the endpoint with the id in the path" in {
      Get("/api/book/2") ~> libraryRoute ~> check {
        response.status shouldBe StatusCodes.OK
        val strictEntityFuture = response.entity.toStrict(1 second)
        val strictEntity = Await.result(strictEntityFuture, 1 second)

        strictEntity.contentType shouldBe ContentTypes.`application/json`
        responseAs[Option[Book]] shouldBe Some(Book(2, "JRR Tolkien", "The Lord of the Rings"))
      }
    }
    "insert a book into the 'database'" in {
      val newBook = Book(5, "Steven Pressfield", "The War of Art")
      Post("/api/book", newBook) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        assert(books.contains(newBook))
        books should contain(newBook)
      }
    }
    "not accept other methods than POST and GET" in {
      Delete("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty
        val methodRejections = rejections.collect {
          case rejection: MethodRejection => rejection
        }
        methodRejections.length shouldBe 2
      }
    }
    "return all the books of a given author" in {
      Get("/api/book/author/JRR%20Tolkien") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Book]] shouldBe books.filter(_.author == "JRR Tolkien")
      }
    }
  }
}
object RouteDSLSpec {
  var books: List[Book] = List(
    Book(1, "Harper Lee", "To Kill a Mockingbird"),
    Book(2, "JRR Tolkien", "The Lord of the Rings"),
    Book(3, "GRR Martin", "A Song of Ice and Fire"),
    Book(4, "Tony Robins", "Awaken the Giant Within")
  )
  implicit val bookFormat: RootJsonFormat[Book] = jsonFormat3(Book)

  val libraryRoute: Route =
    pathPrefix("api" / "book") {
      concat(
        get {
          concat(
            path("author" / Segment) { author =>
              complete(books.filter(_.author == author))
            },
            (path(IntNumber) | parameter("id".as[Int])) { id =>
              complete(books.find(_.id == id))
            },
            pathEndOrSingleSlash {
              complete(books)
            }
          )
        },
        post {
          concat(
            entity(as[Book]) { book =>
              books = books :+ book
              complete(StatusCodes.OK)
            },
            complete(StatusCodes.BadRequest)
          )
        }
      )
    }

}
