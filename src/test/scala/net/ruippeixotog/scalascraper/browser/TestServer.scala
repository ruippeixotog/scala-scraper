package net.ruippeixotog.scalascraper.browser

import java.util.concurrent.atomic.AtomicInteger

import org.http4s.HttpService
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

trait TestServer extends BeforeAfterAll { this: Specification =>

  def testService: HttpService
  lazy val testServerPort: Int = TestServer.nextPort.getAndIncrement()

  private[this] var server = Option.empty[Server]

  def beforeAll() = server = Some(BlazeBuilder.bindHttp(testServerPort).mountService(testService, "/").run)
  def afterAll() = server.map(_.shutdownNow())
}

object TestServer {
  private val nextPort = new AtomicInteger(23464)
}
