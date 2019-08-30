package net.ruippeixotog.scalascraper.browser

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scala.io.Source

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ ActorMaterializer, Materializer }
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

trait TestServer extends BeforeAfterAll { this: Specification =>

  implicit private[this] val system: ActorSystem = ActorSystem("my-system")
  implicit private[this] val materializer: Materializer = ActorMaterializer()
  implicit private[this] val ec: ExecutionContext = system.dispatcher

  def testService: Route
  lazy val testServerPort: Int = TestServer.nextPort.getAndIncrement()

  private[this] var server = Option.empty[ServerBinding]

  def beforeAll(): Unit = {
    server = Some(Await.result(Http().bindAndHandle(testService, "localhost", testServerPort), 5.seconds))
  }

  def afterAll(): Unit = {
    server.foreach(_.unbind().onComplete(_ => system.terminate()))
  }

  def serveText(str: String): Route = {
    complete(HttpEntity(`text/html(UTF-8)`, s"<html><body>$str</body></html>"))
  }

  def serveHtml(html: String): Route = {
    complete(HttpEntity(`text/html(UTF-8)`, html))
  }

  def serveResource(name: String, charset: String = "UTF-8"): Route = {
    val content = Source.fromFile(getClass.getClassLoader.getResource(name).toURI, charset).mkString
    complete(HttpEntity(`text/html(UTF-8)`, content))
  }

  def testServerUri(path: String): String =
    s"http://localhost:$testServerPort/$path"
}

object TestServer {
  private val nextPort = new AtomicInteger(23464)
}
