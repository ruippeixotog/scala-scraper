package net.ruippeixotog.scalascraper.util

import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.SocksTestHelper
import net.ruippeixotog.scalascraper.browser._

@deprecated(
  "Proxies are now configured per Browser instance. See the constructors of JsoupBrowser and " +
    "HtmlUnitBrowser for more information",
  "2.1.0"
)
class ProxyUtilsSpec extends Specification with BrowserHelper with SocksTestHelper {
  sequential

  def withGlobalProxy[T](block: => T): T = {
    ProxyUtils.setSocksProxy(socksProxyHost, socksProxyPort)
    val res = block
    ProxyUtils.removeSocksProxy()
    res
  }

  "A Browser" should {
    usingBrowsersLazy("JsoupBrowser" -> JsoupBrowser.apply _, "HtmlUnitBrowser" -> HtmlUnitBrowser.apply _) { browser =>
      "make requests through a SOCKS server if a global one is configured" in skipIfProxyUnavailable {
        val proxyIP = withGlobalProxy { browser().get("http://ipv4.icanhazip.com").body.text }
        val myIP = browser().get("http://ipv4.icanhazip.com").body.text
        proxyIP !=== myIP
      }
    }
  }
}
