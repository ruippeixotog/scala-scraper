package net.ruippeixotog.scalascraper.browser

import java.io.File

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives.{post => httpPost, _}
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.model._

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
        <span id="t">this is <b>some</b> text with <i>many</i> styles</span>
        <div id="siblings">
          <div id="sibling1">
            1
          </div>
          <div id="sibling2">
            2
          </div>
          <div id="sibling3">
            3
          </div>
          <div id="sibling4">
            4
          </div>
          <div id="sibling5">
            5
          </div>
        </div>
      </body>
    <html>"""

  // format: OFF
  lazy val testService = {
    get {
      path("hello") { serveHtml(html) } ~
      path("encoding" / Segment) { charset =>
        respondWithHeader(`Content-Encoding`.parseFromValueString(charset).right.get) {
          serveResource(s"encoding-${charset.toLowerCase}.html", charset)
        }
      } ~
      path("agent") {
        headerValueByName("User-Agent") { userAgent =>
          serveText(userAgent)
        }
      } ~
      path("redirect") { redirect(Uri(testServerUri("redirected")) , Found) } ~
      path("redirected") { serveText("redirected") } ~
      path("setcookieA") {
        setCookie(HttpCookie("a", "4")) { serveText("cookie set") }
      } ~
      path("setcookieB") {
        setCookie(HttpCookie("b", "5")) { serveText("cookie set") }
      } ~
      path("cookies") {
        optionalHeaderValuePF({ case Cookie(cookies) => cookies }) { cookies =>
          val str = cookies.toSeq.flatten.sortBy(_.name).map { c => s"${c.name}=${c.value}" }.mkString(";")
          serveText(str)
        }
      }
    } ~
    httpPost {
      path("form") {
        formFieldSeq { fields =>
          val dataHtml = fields.map { case (k, vs) => s"""<span id="$k">$vs</span>""" }.mkString
          serveText(dataHtml)
        }
      }
    }
  }
  // format: ON

  "A Browser" should {

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      "do GET requests and parse correctly the returned HTML" in {
        val body = browser.get(testServerUri("hello")).body

        body.tagName mustEqual "body"
        body.children.size mustEqual 4

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "do GET requests and parse correctly the returned HTML with different charsets" in {
        val testPhrase = "El veloz murciélago hindú comía feliz cardillo y kiwi. " +
          "La cigüeña tocaba el saxofón detrás del palenque de paja."

        def getText(doc: Document) = doc.root.select("#a").head.text

        getText(browser.get(testServerUri("encoding/UTF-8"))) mustEqual testPhrase
        getText(browser.get(testServerUri("encoding/ISO-8859-1"))) mustEqual testPhrase
        getText(browser.get(testServerUri("encoding/UTF-16BE"))) mustEqual testPhrase
      }

      "do POST requests and parse correctly the returned HTML" in {
        val params = Map("a" -> "1", "b" -> "bbb", "c1" -> "data")
        val doc = browser.post(testServerUri("form"), params)

        doc.root.select("#a").head.text mustEqual "1"
        doc.root.select("#b").head.text mustEqual "bbb"
        doc.root.select("#c1").head.text mustEqual "data"
      }

      "parse correctly HTML from a string" in {
        val body = browser.parseString(html).body

        body.tagName mustEqual "body"
        body.children.size mustEqual 4

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "parse correctly HTML from a file" in {
        val file = new File(getClass.getResource("/test.html").toURI)
        val body = browser.parseFile(file).body

        body.tagName mustEqual "body"
        body.children.size mustEqual 1

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "parse correctly HTML from a resource" in {
        val body = browser.parseResource("/test.html").body

        body.tagName mustEqual "body"
        body.children.size mustEqual 1

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "parse correctly HTML from files with different charsets" in {
        val testPhrase = "El veloz murciélago hindú comía feliz cardillo y kiwi. " +
          "La cigüeña tocaba el saxofón detrás del palenque de paja."

        def getText(doc: Document) = doc.root.select("#a").head.text

        getText(browser.parseResource("/encoding-utf-8.html")) mustEqual testPhrase
        getText(browser.parseResource("/encoding-utf-8.html", "ISO-8859-1")) must not(beEqualTo(testPhrase))

        getText(browser.parseResource("/encoding-iso-8859-1.html")) must not(beEqualTo(testPhrase))
        getText(browser.parseResource("/encoding-iso-8859-1.html", "ISO-8859-1")) mustEqual testPhrase

        getText(browser.parseResource("/encoding-utf-16be.html")) must throwAn[
          Exception
        ] // file is not valid HTML in UTF-8
        getText(browser.parseResource("/encoding-utf-16be.html", "UTF-16BE")) mustEqual testPhrase
      }

      "do requests with the same user agent as returned by userAgent" in {
        val doc = browser.get(testServerUri("agent"))
        doc.body.text mustEqual browser.userAgent
      }

      "follow redirects" in {
        val doc = browser.get(testServerUri("redirect"))
        doc.location mustEqual testServerUri("redirected")
        doc.body.text mustEqual "redirected"
      }

      "keep and use cookies between requests" in {
        browser.cookies(testServerUri("")) mustEqual Map.empty

        browser.get(testServerUri("setcookieA"))
        val doc = browser.get(testServerUri("cookies"))
        doc.body.text mustEqual "a=4"

        browser.cookies(testServerUri("setcookieA")) mustEqual Map("a" -> "4")
        browser.cookies(testServerUri("")) mustEqual Map("a" -> "4")

        browser.get(testServerUri("setcookieB"))
        val doc2 = browser.get(testServerUri("cookies"))
        doc2.body.text mustEqual "a=4;b=5"

        browser.cookies(testServerUri("")) mustEqual Map("a" -> "4", "b" -> "5")

        browser.clearCookies()
        val doc3 = browser.get(testServerUri("cookies"))
        doc3.body.text mustEqual ""

        browser.cookies(testServerUri("")) mustEqual Map.empty
      }

      "return Document implementations" in {

        "with a correct location method" in {
          val doc = browser.get(testServerUri("hello"))

          doc.location mustEqual testServerUri("hello")
        }

        "with correct title, head and body methods" in {
          val doc = browser.get(testServerUri("hello"))

          doc.title mustEqual "Test Page"
          doc.head.attr("id") mustEqual "hid"
          doc.body.attr("id") mustEqual "bid"
        }

        "with a correct toHtml method" in {
          val doc = browser.get(testServerUri("hello"))
          val docStr = doc.toHtml

          browser.parseString(docStr).toHtml mustEqual docStr
        }
      }

      "return Element implementations" should {

        "with correct parent and children methods" in {
          val doc = browser.parseString(html)

          val body = doc.body
          body.parent must beSome.which { p => p.tagName mustEqual "html" }
          body.children.map(_.tagName) mustEqual Iterable("div", "span", "span", "div")

          val a = doc.root.select("a").head
          a.parent must beSome.which { p => p.attr("id") mustEqual "a1" }
          a.children must beEmpty
        }

        "with correct parent and children methods" in {
          val doc = browser.parseString(html)

          val body = doc.body
          body.parent must beSome.which { p => p.tagName mustEqual "html" }
          body.children.map(_.tagName) mustEqual Iterable("div", "span", "span", "div")

          val a = doc.root.select("a").head
          a.parent must beSome.which { p => p.attr("id") mustEqual "a1" }
          a.children must beEmpty
        }

        "with correct attr, hasAttr and attrs methods" in {
          val doc = browser.parseString(html)

          val body = doc.body
          body.attrs mustEqual Map("id" -> "bid", "data-a" -> "a", "data-b" -> "b")
          body.hasAttr("id") must beTrue
          body.attr("id") mustEqual "bid"
          body.hasAttr("data-b") must beTrue
          body.attr("data-b") mustEqual "b"
          body.hasAttr("data-c") must beFalse
          body.attr("data-c") must throwA[NoSuchElementException]
        }

        "with correct innerHtml and outerHtml methods" in {
          val doc = browser.parseString(html)

          val textNode = doc.root.select("#t").head
          textNode.innerHtml mustEqual "this is <b>some</b> text with <i>many</i> styles"
          textNode.outerHtml mustEqual "<span id=\"t\">this is <b>some</b> text with <i>many</i> styles</span>"
        }

        "with correct siblings methods" in {
          val doc = browser.parseString(html)

          val middleDiv = doc.root.select("#sibling3").head
          val Seq(sibling1, sibling2, sibling4, sibling5) = middleDiv.siblings.toList
          sibling1.attr("id") mustEqual "sibling1"
          sibling2.attr("id") mustEqual "sibling2"
          sibling4.attr("id") mustEqual "sibling4"
          sibling5.attr("id") mustEqual "sibling5"
        }

        "with correct childNodes and siblingNodes methods" in {
          val doc = browser.parseString(html)

          val textNode = doc.root.select("#t").head
          val Seq(b, i) = textNode.select("b, i").toSeq

          textNode.children mustEqual Seq(b, i)
          textNode.childNodes mustEqual Seq(
            TextNode("this is "),
            ElementNode(b),
            TextNode(" text with "),
            ElementNode(i),
            TextNode(" styles")
          )

          b.siblings mustEqual Seq(i)
          b.siblingNodes mustEqual Seq(
            TextNode("this is "),
            TextNode(" text with "),
            ElementNode(i),
            TextNode(" styles")
          )
        }
      }
    }
  }
}
