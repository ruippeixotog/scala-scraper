package net.ruippeixotog.scalascraper.util

import java.net.{ ConnectException, Socket, SocketException }

import net.ruippeixotog.scalascraper.browser._
import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

// This test works only if an external Tor client is running. It tries to connect to localhost:9050; if no service is
// listening in that endpoint, the test is skipped. The test makes a request to http://ipv4.icanhazip.com and the IPs
// returned with and without proxy are compared.
//
// The host and port of the Tor server can be changed by setting the TOR_CLIENT_HOST and TOR_CLIENT_PORT ports. Other
// SOCKS proxies may be used, as long as they execute the request from another external IP.
class ProxyUtilsSpec extends Specification with BrowserHelper with ThrownMessages {
  sequential

  val torClientHost = Option(System.getenv("TOR_CLIENT_HOST")).getOrElse("localhost")
  val torClientPort = Option(System.getenv("TOR_CLIENT_PORT")).fold(9050)(_.toInt)

  def withSocksProxy[T](host: String, port: Int)(block: => T): T = {
    ProxyUtils.setSocksProxy(host, port)
    val res = block
    ProxyUtils.removeSocksProxy()
    res
  }

  def withTorProxy[T](block: => T): T = {
    try {
      new Socket(torClientHost, torClientPort).close() // test if port is open
      withSocksProxy(torClientHost, torClientPort)(block)
    } catch {
      case _: ConnectException | _: SocketException => skip("A Tor proxy is not available")
    }
  }

  "A Browser" should {
    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>

      "make requests through a SOCKS server if configured" in {
        val proxyIP = withTorProxy { browser.get("http://ipv4.icanhazip.com").body.text }
        val myIP = browser.get("http://ipv4.icanhazip.com").body.text
        proxyIP !=== myIP
      }
    }
  }
}
