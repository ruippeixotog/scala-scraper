package net.ruippeixotog.scalascraper.browser

import java.io.File

import org.specs2.mutable.Specification

class BrowserSpec extends Specification with BrowserHelper {

  "A Browser" should {

    val html = """
      <html>
        <head id="hid">
          <title>Test Page</title>
        </head>
        <body id="bid" data-a="a" data-b="b">
          <div id="a1">
            <a></a>
            <div></div>
          </div>
          <span></span>
          <span></span>
        </body>
      <html>"""

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>

      "parse correctly HTML from a string" in {
        val body = browser.parseString(html).body

        body.tagName mustEqual "body"
        body.children.size mustEqual 3

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "parse correctly HTML from a file" in {
        val file = new File(getClass.getClassLoader.getResource("test.html").toURI)
        val body = browser.parseFile(file).body

        body.tagName mustEqual "body"
        body.children.size mustEqual 1

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }
    }
  }
}
