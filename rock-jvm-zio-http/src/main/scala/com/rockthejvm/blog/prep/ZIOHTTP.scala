package com.rockthejvm.blog.prep

import zio.*
import zio.http.*
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method

object ZIOHTTP extends ZIOAppDefault:

  val port = 8080

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hello, owls!")
  }

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3,5).map(n => Response.text("Hello " * n + ", owls!"))
  }

  val combined = app ++ zApp

  // middleware
  val wrapped = (combined @@ Middleware.debug).withDefaultErrorResponse

  val logging = (combined @@ Verbose.log).withDefaultErrorResponse

  val corsConfig = CorsConfig(
    anyOrigin = false,
    anyMethod = false,
    allowedOrigins = s => s.equals("localhost"),
    allowedMethods = Some(Set(Method.GET, Method.POST))
  )

  val corsEnabledHttp = (combined @@ Middleware.cors(corsConfig) @@ Verbose.log).withDefaultErrorResponse

  // CSRF
  // logged in to online store
  // browser plugin => display a button => when clicked, trigger a form submission
  // CSRF token + cookie = double submit cookie

  val httpProgram =
    for
      _ <- Console.printLine(s"Starting server at http://localhost:$port")
      _ <- Server.serve(corsEnabledHttp).provide(Server.default)
    yield ()

  override def run: ZIO[Any, Any, Any] = httpProgram

object Verbose:
  def log[R, E >: Exception]: Middleware[R, E, Request, Response, Request, Response] = new Middleware[R, E, Request, Response, Request, Response] {
    override def apply[R1 <: R, Err1 >: E](http: Http[R1, Err1, Request, Response])(implicit trace: Trace): Http[R1, Err1, Request, Response] =
      http
        .mapZIO[R1, Err1, Response](r =>
          for
            _ <- Console.printLine(s"< ${r.status}")
            _ <- ZIO.foreach(r.headers.toList) { header =>
              Console.printLine(s"< ${header._1} ${header._2}")
            }
          yield r
        )
  }
