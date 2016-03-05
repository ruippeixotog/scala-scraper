package net.ruippeixotog.scalascraper.browser

import org.http4s.{ Uri, HttpService }
import org.http4s.dsl._
import org.specs2.mutable.Specification

class JsoupBrowserSpec extends Specification with TestServer {

  val emptyHtml = "<html><head></head><body></body></html>"

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

  def uri(uriStr: String) = Uri.fromString(uriStr).validation.getOrElse(throw new Exception)

  lazy val testService = HttpService {
    case req @ GET -> Root / "hello" => Ok(html)
    case req @ GET -> Root / "empty" => Ok(emptyHtml)

    case req @ GET -> Root / "agent" =>
      val userAgent = req.headers.get("User-Agent".ci).fold("")(_.value)
      Ok(s"<html><body>$userAgent</body></html>")

    case GET -> Root / "redirect" => Found(uri(s"http://localhost:$testServerPort/redirected"))
    case GET -> Root / "redirected" => Ok(html)

    case GET -> Root / "setcookieA" => Ok("cookie set").addCookie("a", "4")
    case GET -> Root / "setcookieB" => Ok("cookie set").addCookie("b", "5")
  }

  "A JsoupBrowser" should {

    "execute requests with the specified user agent" in {
      val browser = new JsoupBrowser("test-agent")

      val doc = browser.get(s"http://localhost:$testServerPort/agent")
      doc.body.text mustEqual "test-agent"
    }

    "follow redirects specified in 'Location' headers" in {
      val browser = new JsoupBrowser()

      val doc = browser.get(s"http://localhost:$testServerPort/redirect")
      doc.location mustEqual s"http://localhost:$testServerPort/redirected"
      doc.body.attr("id") mustEqual "bid"
    }

    "keep and use cookies between requests" in {
      val browser = new JsoupBrowser()

      browser.get(s"http://localhost:$testServerPort/setcookieA")
      browser.cookies.get("a") must beSome("4")
      browser.cookies.get("b") must beNone

      browser.get(s"http://localhost:$testServerPort/setcookieB")
      browser.cookies.get("a") must beSome("4")
      browser.cookies.get("b") must beSome("5")
    }
  }

  "A JsoupBrowser's Document" should {

    "have a correct location method" in {
      val browser = new JsoupBrowser()
      val doc = browser.get(s"http://localhost:$testServerPort/hello")

      doc.location mustEqual s"http://localhost:$testServerPort/hello"
    }

    "have correct title, head and body methods" in {
      val browser = new JsoupBrowser()
      val doc = browser.get(s"http://localhost:$testServerPort/hello")

      doc.title mustEqual "Test Page"
      doc.head.attr("id") mustEqual "hid"
      doc.body.attr("id") mustEqual "bid"
    }

    "have a correct toHtml method" in {
      val browser = new JsoupBrowser()
      val doc = browser.get(s"http://localhost:$testServerPort/empty")

      doc.toHtml.filterNot(_.isWhitespace) mustEqual emptyHtml
    }
  }

  "A JsoupBrowser's Element" should {

    "have correct parent and children methods" in {
      val doc = JsoupBrowser().parseString(html)

      val body = doc.body
      body.parent must beSome.which { p => p.tagName mustEqual "html" }
      body.children.map(_.tagName) mustEqual Iterable("div", "span", "span")

      val a = doc.root.select("a").head
      a.parent must beSome.which { p => p.attr("id") mustEqual "a1" }
      a.children must beEmpty
    }

    "have correct attr and attrs methods" in {
      val doc = JsoupBrowser().parseString(html)

      val body = doc.body
      body.attrs mustEqual Map("id" -> "bid", "data-a" -> "a", "data-b" -> "b")
      body.attr("id") mustEqual "bid"
      body.attr("data-b") mustEqual "b"
    }

    "have correct innerHtml and outerHtml methods" in {
      val doc = JsoupBrowser().parseString(html)

      val title = doc.root.select("title").head
      title.innerHtml mustEqual "Test Page"
      title.outerHtml mustEqual "<title>Test Page</title>"
    }
  }
}
