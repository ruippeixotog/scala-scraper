package net.ruippeixotog.scalascraper.browser

import java.io.{ ByteArrayInputStream, File }
import javax.xml.parsers.DocumentBuilderFactory

import org.http4s.HttpService
import org.http4s.dsl._
import org.specs2.mutable.Specification

class BrowserSpec extends Specification with BrowserHelper with TestServer {

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
        <span id="t">this is <b>some</b> text</span>
      </body>
    <html>"""

  lazy val testService = HttpService {
    case req @ GET -> Root / "hello" => Ok(html)
  }

  "A Browser" should {

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

      "return Document implementations" in {

        "with a correct location method" in {
          val doc = browser.get(s"http://localhost:$testServerPort/hello")

          doc.location mustEqual s"http://localhost:$testServerPort/hello"
        }

        "with correct title, head and body methods" in {
          val doc = browser.get(s"http://localhost:$testServerPort/hello")

          doc.title mustEqual "Test Page"
          doc.head.attr("id") mustEqual "hid"
          doc.body.attr("id") mustEqual "bid"
        }

        "with a correct toHtml method" in {
          val doc = browser.get(s"http://localhost:$testServerPort/hello")
          val docStr = doc.toHtml

          browser.parseString(docStr).toHtml mustEqual docStr
        }
      }

      "return Element implementations" should {

        "with correct parent and children methods" in {
          val doc = browser.parseString(html)

          val body = doc.body
          body.parent must beSome.which { p => p.tagName mustEqual "html" }
          body.children.map(_.tagName) mustEqual Iterable("div", "span", "span")

          val a = doc.root.select("a").head
          a.parent must beSome.which { p => p.attr("id") mustEqual "a1" }
          a.children must beEmpty
        }

        "with correct attr and attrs methods" in {
          val doc = browser.parseString(html)

          val body = doc.body
          body.attrs mustEqual Map("id" -> "bid", "data-a" -> "a", "data-b" -> "b")
          body.attr("id") mustEqual "bid"
          body.attr("data-b") mustEqual "b"
        }

        "with correct innerHtml and outerHtml methods" in {
          val doc = browser.parseString(html)

          val textNode = doc.root.select("#t").head
          textNode.innerHtml mustEqual "this is <b>some</b> text"
          textNode.outerHtml mustEqual "<span id=\"t\">this is <b>some</b> text</span>"
        }
      }
    }
  }
}
