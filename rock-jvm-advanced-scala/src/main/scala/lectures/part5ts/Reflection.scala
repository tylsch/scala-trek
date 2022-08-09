package lectures.part5ts

object Reflection extends App {
  // Reflection + macros + quasiquotes => METAPROGRAMMING
  case class Person(name: String) {
    def sayMyName(): Unit = println(s"Hi, my name is $name")
  }

  // 0 - import
  import scala.reflect.runtime.{universe => ru}

  // 1 - Mirror
  val m = ru.runtimeMirror(getClass.getClassLoader)
  // 2 - Create a class object == "description"
  val clazz = m.staticClass("lectures.part5ts.Reflection.Person") // creating a class object by NAME
  // 3 - create a reflected mirror = "can DO things"
  val cm = m.reflectClass(clazz)
  // 4 - get the CONSTRUCTOR
  val constructor = clazz.primaryConstructor.asMethod
  // 5 - reflect the CONSTRUCTOR
  val constructorMirror = cm.reflectConstructor(constructor)
  // 6 - invoke the CONSTRUCTOR
  val instance = constructorMirror.apply("John")

  println(instance)

  // I have an instance
  val p = Person("Mary") // from the wire as a serialized object
  // method name computed from somewhere else
  val methodName = "sayMyName"
  // 1 - mirror
  // 2 - reflect the instance
  val reflected = m.reflect(p)
  // 3 - method symbol
  val methodSymbol = ru.typeOf[Person].decl(ru.TermName(methodName)).asMethod
  // 4 - reflect the method
  val method = reflected.reflectMethod(methodSymbol)
  // 5 - invoke the method
  method.apply()
}
