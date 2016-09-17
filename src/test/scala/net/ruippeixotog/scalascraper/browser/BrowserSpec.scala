package net.ruippeixotog.scalascraper.browser

import java.io.File

import net.ruippeixotog.scalascraper.model.Document
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Content-Encoding`
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

  def uri(uriStr: String) = Uri.fromString(uriStr).validation.getOrElse(throw new Exception)

  lazy val testService = HttpService {
    case GET -> Root / "hello" => Ok(html)
    case GET -> Root / "encoding" / charset =>
      serveResource(s"encoding-${charset.toLowerCase}.html", charset)
        .putHeaders(`Content-Encoding`(charset.ci))

    case req @ POST -> Root / "form" =>
      req.decode[UrlForm] { form =>
        val dataHtml = form.values.map { case (k, vs) => s"""<span id="$k">${vs.head}</span>""" }.mkString
        serveText(dataHtml)
      }

    case req @ GET -> Root / "agent" =>
      val userAgent = req.headers.get("User-Agent".ci).fold("")(_.value)
      serveText(userAgent)

    case GET -> Root / "redirect" => Found(uri(testServerUri("redirected")))
    case GET -> Root / "redirected" => serveText("redirected")

    case GET -> Root / "setcookieA" => Ok("cookie set").putHeaders(Header.Raw("Set-Cookie".ci, "a=4"))
    case GET -> Root / "setcookieB" => Ok("cookie set").putHeaders(Header.Raw("Set-Cookie".ci, "b=5"))
    case req @ GET -> Root / "cookies" =>
      val cookies = req.headers.get(headers.Cookie).toSeq.flatMap(_.values.list).sortBy(_.name)
      val cookiesStr = cookies.map { c => s"${c.name}=${c.content}" }.mkString(";")
      serveText(cookiesStr)
  }

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
        getText(browser.parseResource(
          "/encoding-utf-8.html",
          "ISO-8859-1")) must not(beEqualTo(testPhrase))

        getText(browser.parseResource("/encoding-iso-8859-1.html")) must not(beEqualTo(testPhrase))
        getText(browser.parseResource("/encoding-iso-8859-1.html", "ISO-8859-1")) mustEqual testPhrase

        getText(browser.parseResource("/encoding-utf-16be.html")) must throwAn[Exception] // file is not valid HTML in UTF-8
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
          textNode.innerHtml mustEqual "this is <b>some</b> text"
          textNode.outerHtml mustEqual "<span id=\"t\">this is <b>some</b> text</span>"
        }

        "with correct siblings methods" in {
          val doc = browser.parseString(html)

          val middleDiv = doc.root.select("#sibling3").head
          val Seq(sibling1, sibling2, sibling4, sibling5) = middleDiv.siblings
          sibling1.attr("id") mustEqual "sibling1"
          sibling2.attr("id") mustEqual "sibling2"
          sibling4.attr("id") mustEqual "sibling4"
          sibling5.attr("id") mustEqual "sibling5"
        }
      }
    }
  }
}
