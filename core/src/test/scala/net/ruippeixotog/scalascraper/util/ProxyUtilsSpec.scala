package net.ruippeixotog.scalascraper.util

import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.SocksTestHelper
import net.ruippeixotog.scalascraper.browser._

// This test works only if an external Tor client is running. It tries to connect to localhost:9050; if no service is
// listening in that endpoint, the test is skipped. The test makes a request to http://ipv4.icanhazip.com and the IPs
// returned with and without proxy are compared.
//
// The host and port of the Tor server can be changed by setting the TOR_CLIENT_HOST and TOR_CLIENT_PORT ports. Other
// SOCKS proxies may be used, as long as they execute the request from another external IP.
class ProxyUtilsSpec extends Specification with BrowserHelper with SocksTestHelper {
  sequential

  def withGlobalProxy[T](block: => T): T = {
    ProxyUtils.setSocksProxy(socksProxyHost, socksProxyPort)
    val res = block
    ProxyUtils.removeSocksProxy()
    res
  }

  "A Browser" should {
    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>

      "make requests through a SOCKS server if a global one is configured" in skipIfProxyUnavailable {
        val proxyIP = withGlobalProxy { browser.get("http://ipv4.icanhazip.com").body.text }
        val myIP = browser.get("http://ipv4.icanhazip.com").body.text
        proxyIP !=== myIP
      }
    }
  }
}
