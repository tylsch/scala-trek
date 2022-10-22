package part3Graphs

import akka.actor.ActorSystem
import akka.stream.{BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")

  /*
  * Example: Cryptography
  * */

  def encrypt(n: Int)(string: String) = string.map(c => (c + n).toChar)
  def decrypt(n: Int)(string: String) = string.map(c => (c - n).toChar)

  val bidiCryptoStaticGraph = GraphDSL.create() {
    implicit builder =>

      val encryptionFlow = builder.add(Flow[String].map(encrypt(3)))
      val decryptionFlow = builder.add(Flow[String].map(decrypt(3)))

      BidiShape.fromFlows(encryptionFlow, decryptionFlow)
  }

  val unencryptedStrings = List("akka", "is", "awesome", "testing")
  val unencryptedSource = Source(unencryptedStrings)
  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val unencryptedSourceShape = builder.add(unencryptedSource)
        val encryptedSourceShape = builder.add(encryptedSource)
        val bidi = builder.add(bidiCryptoStaticGraph)
        val encryptedSink = builder.add(Sink.foreach[String](s => println(s"Encrypted: $s")))
        val decryptedSink = builder.add(Sink.foreach[String](s => println(s"Decrypted: $s")))

        unencryptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptedSink
        decryptedSink         <~ bidi.out2  ; bidi.in2 <~ encryptedSourceShape

        ClosedShape
    }
  )
  cryptoBidiGraph.run()
}
