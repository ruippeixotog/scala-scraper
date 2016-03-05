package net.ruippeixotog.scalascraper.browser

import org.http4s.{ Uri, HttpService }
import org.http4s.dsl._
import org.specs2.mutable.Specification

class JsoupBrowserSpec extends Specification with TestServer {

  def uri(uriStr: String) = Uri.fromString(uriStr).validation.getOrElse(throw new Exception)

  lazy val testService = HttpService {
    case req @ GET -> Root / "agent" =>
      val userAgent = req.headers.get("User-Agent".ci).fold("")(_.value)
      Ok(s"<html><body>$userAgent</body></html>")

    case GET -> Root / "redirect" => Found(uri(s"http://localhost:$testServerPort/redirected"))
    case GET -> Root / "redirected" => Ok("""<html><body>redirected</body><html>""")

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
      doc.body.text mustEqual "redirected"
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
}
