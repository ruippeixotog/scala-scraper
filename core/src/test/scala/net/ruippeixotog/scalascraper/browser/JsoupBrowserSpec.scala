package net.ruippeixotog.scalascraper.browser

import java.net.{InetSocketAddress, Proxy}

import akka.http.scaladsl.server.Directives._
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.SocksTestHelper

class JsoupBrowserSpec extends Specification with TestServer {

  lazy val testService = get {
    path("agent") {
      headerValueByName("User-Agent") { userAgent =>
        serveText(userAgent)
      }
    }
  }

  "A JsoupBrowser" should {

    "execute requests with the specified user agent" in {
      val browser = new JsoupBrowser("test-agent")
      browser.userAgent mustEqual "test-agent"

      val doc = browser.get(testServerUri("agent"))
      doc.body.text mustEqual "test-agent"
    }
  }
}
