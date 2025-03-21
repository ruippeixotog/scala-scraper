package net.ruippeixotog.scalascraper.browser

import akka.http.scaladsl.server.Directives._
import org.jsoup.parser.{ParseSettings, Parser}
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.util.LineCompare

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

    "pass custom html parser with parse settings" in {

      def parserBuilder(): Parser = {
        val parser = Parser.htmlParser()
        parser.settings(new ParseSettings(true, true))
        parser
      }

      val browser = new JsoupBrowser(parserBuilder = parserBuilder)

      val html = """
        |<html>
        |  <head>
        |    <title>Test</title>
        |  </head>
        |  <body>
        |    <div id="content" customSnakeTag="testValue">
        |      <p>Some text</p>
        |    </div>
        |  </body>
        |</html>
      """.stripMargin

      val parseResult = browser.parseString(html).toHtml

      LineCompare.compare(html, parseResult) mustEqual true
    }
  }
}
