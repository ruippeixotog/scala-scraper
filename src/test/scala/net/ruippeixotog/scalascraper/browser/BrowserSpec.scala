package net.ruippeixotog.scalascraper.browser

import java.io.File

import org.http4s.{ UrlForm, headers, Uri, HttpService }
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

  def uri(uriStr: String) = Uri.fromString(uriStr).validation.getOrElse(throw new Exception)
  def wrapHtml(str: String) = s"<html><body>$str</body></html>"

  lazy val testService = HttpService {
    case GET -> Root / "hello" => Ok(html)

    case req @ POST -> Root / "form" =>
      req.decode[UrlForm] { form =>
        val dataHtml = form.values.map { case (k, vs) => s"""<span id="$k">${vs.head}</span>""" }.mkString
        Ok(wrapHtml(dataHtml))
      }

    case GET -> Root / "redirect" => Found(uri(s"http://localhost:$testServerPort/redirected"))
    case GET -> Root / "redirected" => Ok(wrapHtml("redirected"))

    case GET -> Root / "setcookieA" => Ok("cookie set").addCookie("a", "4")
    case GET -> Root / "setcookieB" => Ok("cookie set").addCookie("b", "5")
    case req @ GET -> Root / "cookies" =>
      val cookies = req.headers.get(headers.Cookie).toSeq.flatMap(_.values.list).sortBy(_.name)
      val cookiesStr = cookies.map { c => s"${c.name}=${c.content}" }.mkString(";")
      Ok(wrapHtml(cookiesStr))
  }

  "A Browser" should {

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>

      "do GET requests and parse correctly the returned HTML" in {
        val body = browser.get(s"http://localhost:$testServerPort/hello").body

        body.tagName mustEqual "body"
        body.children.size mustEqual 3

        val div = body.children.head
        div.tagName mustEqual "div"
        div.attr("id") mustEqual "a1"
        div.children.size mustEqual 2
      }

      "do POST requests and parse correctly the returned HTML" in {
        val params = Map("a" -> "1", "b" -> "bbb", "c1" -> "data")
        val doc = browser.post(s"http://localhost:$testServerPort/form", params)

        doc.root.select("#a").head.text mustEqual "1"
        doc.root.select("#b").head.text mustEqual "bbb"
        doc.root.select("#c1").head.text mustEqual "data"
      }

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

      "follow redirects" in {
        val doc = browser.get(s"http://localhost:$testServerPort/redirect")
        doc.location mustEqual s"http://localhost:$testServerPort/redirected"
        doc.body.text mustEqual "redirected"
      }

      "keep and use cookies between requests" in {
        browser.get(s"http://localhost:$testServerPort/setcookieA")
        val doc = browser.get(s"http://localhost:$testServerPort/cookies")
        doc.body.text mustEqual "a=4"

        browser.get(s"http://localhost:$testServerPort/setcookieB")
        val doc2 = browser.get(s"http://localhost:$testServerPort/cookies")
        doc2.body.text mustEqual "a=4;b=5"
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
