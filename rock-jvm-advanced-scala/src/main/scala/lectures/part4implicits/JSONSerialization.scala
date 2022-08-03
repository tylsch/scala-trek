package lectures.part4implicits

import java.util.Date

object JSONSerialization extends App {
  /*
  * Users, posts, feeds
  * Serialize to JSON
  * */

  case class User(name: String, age: Int, email: String)
  case class Post(content: String, createdAt: Date)
  case class Feed(user: User, posts: List[Post])

  /*
  * 1) Intermediate Data Types: Int, String, List, Date
  * 2) Type Class for conversion to intermediate data types
  * 3) Serialize to JSON
  * */

  sealed trait JsonValue {
    def stringify: String
  }

  final case class JsonString(value: String) extends JsonValue {
    override def stringify: String = s"\"$value\""
  }
  final case class JsonNumber(value: Int) extends JsonValue {
    override def stringify: String = value.toString
  }
  final case class JsonArray(value: List[JsonValue]) extends JsonValue {
    override def stringify: String = value.map(_.stringify).mkString("[", ",", "]")
  }
  final case class JsonObject(values: Map[String, JsonValue]) extends JsonValue {
    override def stringify: String = values.map {
      case (key, value) => s"\"$key\":${value.stringify}"
    }.mkString("{", ",", "}")
  }

  val data = JsonObject(Map(
    "user" -> JsonString("Daniel"),
    "posts" -> JsonArray(List(
      JsonString("Scala Rocks!"),
      JsonNumber(453)
    ))
  ))
  println(data.stringify)

  // type class
  trait JsonConverter[T] {
    def convert(value: T): JsonValue
  }

  implicit class JsonOps[T](value: T) {
    def toJson(implicit converter: JsonConverter[T]): JsonValue =
      converter.convert(value)
  }

  implicit object StringConverter extends JsonConverter[String] {
    override def convert(value: String): JsonValue = JsonString(value)
  }
  implicit object NumberConverter extends JsonConverter[Int] {
    override def convert(value: Int): JsonValue = JsonNumber(value)
  }

  // custom data types
  implicit object UserConverter extends JsonConverter[User] {
    override def convert(value: User): JsonValue = JsonObject(Map(
      "name" -> JsonString(value.name),
      "age" -> JsonNumber(value.age),
      "email" -> JsonString(value.email)
    ))
  }
  implicit object PostConverter extends JsonConverter[Post] {
    override def convert(value: Post): JsonValue = JsonObject(Map(
      "content" -> JsonString(value.content),
      "createdAt" -> JsonString(value.createdAt.toString)
    ))
  }
  implicit object FeedConverter extends JsonConverter[Feed] {
    override def convert(value: Feed): JsonValue = JsonObject(Map(
      "user" -> value.user.toJson,
      "posts" -> JsonArray(value.posts.map(_.toJson))
    ))
  }

  // call stringify on result
  val now = new Date(System.currentTimeMillis())
  val john = User("John", 34, "john@rockthejvm.com")
  val feed = Feed(john, List(
    Post("hello", now),
    Post("look at this cute puppy", now)
  ))
  println(feed.toJson.stringify)
}
