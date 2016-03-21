package net.ruippeixotog.scalascraper.browser

import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source

import org.http4s.HttpService
import org.http4s.dsl._
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

trait TestServer extends BeforeAfterAll { this: Specification =>

  def testService: HttpService
  lazy val testServerPort: Int = TestServer.nextPort.getAndIncrement()

  private[this] var server = Option.empty[Server]

  def beforeAll() = server = Some(BlazeBuilder.bindHttp(testServerPort).mountService(testService, "/").run)
  def afterAll() = server.foreach(_.shutdownNow())

  def serveText(str: String) = Ok(s"<html><body>$str</body></html>")

  def serveResource(name: String, charset: String = "UTF-8") = {
    val content = Source.fromFile(getClass.getClassLoader.getResource(name).toURI, charset).mkString
    Ok(content)
  }

  def testServerUri(path: String) = s"http://localhost:$testServerPort/$path"
}

object TestServer {
  private val nextPort = new AtomicInteger(23464)
}
