import java.util.UUID

object StringTask {
  def strTask(): String = {
    UUID.randomUUID.toString()
  }
}
