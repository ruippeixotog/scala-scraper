package net.ruippeixotog.scalascraper.browser

import java.io.File

import akka.http.scaladsl.server.Directives._
import com.gargoylesoftware.htmlunit.{ BrowserVersion, ProxyConfig }
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.SocksTestHelper

class HtmlUnitBrowserSpec extends Specification with TestServer with SocksTestHelper {

  // format: OFF
  lazy val testService = get {
    path("agent") {
      headerValueByName("User-Agent") { userAgent =>
        serveText(userAgent)
      }
    } ~
    path("jsredirect") { serveResource("testjs2.1.html") } ~
    path("jsredirected") { serveResource("testjs2.2.html") }
  }
  // format: ON

  "An HtmlUnitBrowser" should {

    "execute requests with the user agent for the chosen browser" in {
      val browser = new HtmlUnitBrowser(BrowserVersion.FIREFOX_60)
      browser.userAgent must contain("Firefox/60.0")

      val doc = browser.get(s"http://localhost:$testServerPort/agent")
      doc.body.text mustEqual browser.userAgent
    }

    "execute JavaScript in HTML pages" in {
      val file = new File(getClass.getClassLoader.getResource("testjs.html").toURI)
      val doc = new HtmlUnitBrowser().parseFile(file)

      doc.root.select("#t").head.text mustEqual "Before"
      doc.root.select("#t").head.text must beEqualTo("After").eventually
    }

    "keep the documented mutability semantics on element changes" in {
      val file = new File(getClass.getClassLoader.getResource("testjs.html").toURI)

      val doc = new HtmlUnitBrowser().parseFile(file)
      val elem = doc.root.select("#t").head

      doc.root.select("#t").head.text mustEqual "Before"
      elem.text mustEqual "Before"

      eventually {
        doc.root.select("#t").head.text mustEqual "After"
        elem.text mustEqual "After"
      }
    }

    "support JavaScript redirections" in {
      val doc = new HtmlUnitBrowser().get(s"http://localhost:$testServerPort/jsredirect")

      doc.location mustEqual s"http://localhost:$testServerPort/jsredirect"
      doc.root.select("#t").head.text mustEqual "First"

      doc.location must beEqualTo(s"http://localhost:$testServerPort/jsredirected").eventually
      doc.root.select("#t").head.text mustEqual "Second"
    }

    "keep the documented mutability semantics on page refreshes and redirects" in {
      val doc = new HtmlUnitBrowser().get(s"http://localhost:$testServerPort/jsredirect")
      val elem = doc.root.select("#t").head

      doc.location mustEqual s"http://localhost:$testServerPort/jsredirect"
      doc.root.select("#t").head.text mustEqual "First"
      elem.text mustEqual "First"

      eventually {
        doc.location mustEqual s"http://localhost:$testServerPort/jsredirected"
        doc.root.select("#t").head.text mustEqual "Second"
        elem.text mustEqual "Between" // Element from an outdated element tree
      }
    }

    "allow closing all the pages opened in it" in {
      val file = new File(getClass.getClassLoader.getResource("testjs.html").toURI)
      val browser = new HtmlUnitBrowser()
      val doc = browser.parseFile(file)

      doc.root.select("#t").head.text mustEqual "Before"
      browser.closeAll()
      doc.root.select("#t").head.text must not(beEqualTo("After").eventually)
    }

    "make requests through a SOCKS server if configured" in skipIfProxyUnavailable {
      val proxyConfig = new ProxyConfig(socksProxyHost, socksProxyPort, true)

      val browser = new HtmlUnitBrowser()
      val proxiedBrowser = new HtmlUnitBrowser(proxy = proxyConfig)

      val proxyIP = proxiedBrowser.get("http://ipv4.icanhazip.com").body.text
      val myIP = browser.get("http://ipv4.icanhazip.com").body.text
      proxyIP !=== myIP
    }
  }
}
