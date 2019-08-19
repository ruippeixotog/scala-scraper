package net.ruippeixotog.scalascraper.browser

import java.util.concurrent.atomic.AtomicInteger

import cats.effect.{ CancelToken, ContextShift, IO, Timer }

import scala.io.Source
import org.http4s.{ Header, Headers, HttpRoutes, HttpService, Response, Status }
import org.http4s.dsl._
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.implicits._
import org.http4s.server.blaze.{ BlazeBuilder, BlazeServerBuilder }
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scala.concurrent.ExecutionContext

trait TestServer extends BeforeAfterAll { this: Specification =>

  private val executionContext: ExecutionContext = ExecutionContext.global

  implicit val ioContextShift: ContextShift[IO] = IO.contextShift(executionContext)
  implicit val ioTimer: Timer[IO] = IO.timer(executionContext)

  def testService: HttpRoutes[IO]
  lazy val testServerPort: Int = TestServer.nextPort.getAndIncrement()

  private[this] var server = Option.empty[CancelToken[IO]]

  def beforeAll(): Unit = {
    server = Some(BlazeServerBuilder[IO]
      .withHttpApp(Router("/" -> testService).orNotFound)
      .bindHttp(testServerPort)
      .serve
      .compile
      .drain
      .unsafeRunCancelable(println))
  }

  def afterAll() = server.foreach(_.unsafeRunSync())

  def serveText(str: String) = Ok(s"<html><body>$str</body></html>")

  def serveResource(name: String, charset: String = "UTF-8", headers: List[Header] = List.empty): IO[Response[IO]] = {
    val content = Source.fromFile(getClass.getClassLoader.getResource(name).toURI, charset).mkString

    IO(Response(Status.Ok, headers = Headers.of(headers: _*)).withEntity(content))
  }

  def testServerUri(path: String) = s"http://localhost:$testServerPort/$path"
}

object TestServer {
  private val nextPort = new AtomicInteger(23464)
}
