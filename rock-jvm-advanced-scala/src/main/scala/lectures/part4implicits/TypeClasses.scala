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

  implicit object UserSerializer extends HtmlSerializer[User] {
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

  // Part 2
  object HtmlSerializer {
    def serialize[T](value: T)(implicit serializer: HtmlSerializer[T]): String = serializer.serialize(value)

    def apply[T](implicit serializer: HtmlSerializer[T]): HtmlSerializer[T] = serializer
  }

  implicit object IntSerializer extends HtmlSerializer[Int] {
    override def serialize(value: Int): String = s"<div style: color=blue>$value</div>"
  }

  println(HtmlSerializer.serialize(42))
  println(HtmlSerializer.serialize(john))

  // access to the entire type class interface
  println(HtmlSerializer[User].serialize(john))

  // part 3
  implicit class HtmlEnrichment[T](value: T) {
    def toHtml(implicit serializer: HtmlSerializer[T]): String = serializer.serialize(value)
  }

  println(john.toHtml)
  /*
  * Extend to new types
  * */

  println(2.toHtml)
  //println(john.toHtml(ParitalUserSerializer))

  // context bounds
  def htmlBoilerplate[T](content: T)(implicit serializer: HtmlSerializer[T]): String =
    s"<html><body>${content.toHtml(serializer)}</body></html>"

  def htmlSugar[T : HtmlSerializer](content: T): String = {
    val serializer = implicitly[HtmlSerializer[T]]
    s"<html><body>${content.toHtml(serializer)}</body></html>"
  }

  // implicitly
  case class Permissions(mask: String)
  implicit val defaultPermissions: Permissions = Permissions("0744")

  // in some other part of the code
  val standardPerms = implicitly[Permissions]

}
