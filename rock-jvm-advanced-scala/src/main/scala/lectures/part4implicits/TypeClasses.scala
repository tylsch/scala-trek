package lectures.part4implicits

object TypeClasses extends App {
  trait HtmlWritable {
    def toHtml: String
  }

  case class User(name: String, age: Int, email: String) extends HtmlWritable {
    override def toHtml: String = s"<div>$name ($age yo) <a href=$email/></div>"
  }

  val john = User("John", 32, "john@rockthejvm.com")
  /*
  * 1 - for the types WE write
  * 2 - ONE implementation out of quite a number
  * */

  // option 2 - pattern matching
//  object HtmlSerializerPM {
//    def serializeToHtml(value: Any) = value match {
//      case User(n, a, e) =>
//      case java.util.Date =>
//      case _ =>
//    }
//  }
  /*
  * 1 - lost type safety
  * 2 - need to modify code every time
  * 3 - still ONE implementation
  * */

  trait HtmlSerializer[T] {
    def serialize(value: T): String
  }

  object UserSerializer extends HtmlSerializer[User] {
    override def serialize(user: User): String = s"<div>${user.name} (${user.age} yo) <a href=${user.email}/></div>"
  }

  println(UserSerializer.serialize(john))

  // 1 - we can define serializers for other types
  import java.util.Date
  object DateSerializer extends HtmlSerializer[Date] {
    override def serialize(value: Date): String = s"<div>${value.toString}</div>"
  }

  // 2 - we can define multiple serializers
  object ParitalUserSerializer extends HtmlSerializer[User] {
    override def serialize(user: User): String = s"<div>${user.name}</div>"
  }

  // TYPE Class
  trait MyTypeClassTemplate[T] {
    def action(value: T): String
  }

  /*
  * Equality
  * */
  trait Equal[T] {
    def apply(a: T, b: T): Boolean
  }

  object NameEquality extends Equal[User] {
    override def apply(a: User, b: User): Boolean = a.name == b.name
  }
  object FullEquality extends Equal[User] {
    override def apply(a: User, b: User): Boolean = a.name == b.name && a.email == b.email
  }
}
