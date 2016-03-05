package net.ruippeixotog.scalascraper.browser

import org.http4s.HttpService
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeBuilder
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

trait TestServer extends BeforeAfterAll { this: Specification =>

  def testService: HttpService
  def testServerPort: Int = 23464

  private[this] var server = Option.empty[Server]

  def beforeAll() = server = Some(BlazeBuilder.bindHttp(testServerPort).mountService(testService, "/").run)
  def afterAll() = server.map(_.shutdownNow())
}
