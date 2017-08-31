package net.ruippeixotog.scalascraper.browser

import java.net.{ InetSocketAddress, Proxy }

import org.http4s.HttpService
import org.http4s.dsl._
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.SocksTestHelper

class JsoupBrowserSpec extends Specification with TestServer with SocksTestHelper {

  lazy val testService = HttpService {
    case req @ GET -> Root / "agent" =>
      val userAgent = req.headers.get("User-Agent".ci).fold("")(_.value)
      serveText(userAgent)
  }

  "A JsoupBrowser" should {

    "execute requests with the specified user agent" in {
      val browser = new JsoupBrowser("test-agent")
      browser.userAgent mustEqual "test-agent"

      val doc = browser.get(testServerUri("agent"))
      doc.body.text mustEqual "test-agent"
    }

    "make requests through a SOCKS server if configured" in skipIfProxyUnavailable {
      val proxyConfig = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksProxyHost, socksProxyPort))

      val browser = new JsoupBrowser()
      val proxiedBrowser = new JsoupBrowser(proxy = proxyConfig)

      val proxyIP = proxiedBrowser.get("http://ipv4.icanhazip.com").body.text
      val myIP = browser.get("http://ipv4.icanhazip.com").body.text
      proxyIP !=== myIP
    }
  }
}
