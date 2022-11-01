package part2_lowlevelserver

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsContext {
  // key store
  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystoreFile: InputStream = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
  val password: Array[Char] = "akka-https".toCharArray // fetch the password from a secure place
  ks.load(keystoreFile, password)

  // Step 2: initialize a key manager
  val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  // Step 3: initialize a trust manager
  val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(ks)

  // Step 4: initialize an SSL context
  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

  // Step 5: return the https connection context
  val httpsConnectionContext: HttpsConnectionContext = ConnectionContext.httpsServer(sslContext)
}

object LowLevelHttps extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "LowLevelHttps")
  
  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP!
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Resource can't be found
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpsBinding = Http().newServerAt("localhost", 8443).enableHttps(HttpsContext.httpsConnectionContext).bindSync(requestHandler)
}
