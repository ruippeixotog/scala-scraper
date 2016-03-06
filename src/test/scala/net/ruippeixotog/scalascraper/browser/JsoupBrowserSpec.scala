package net.ruippeixotog.scalascraper.browser

import org.http4s.HttpService
import org.http4s.dsl._
import org.specs2.mutable.Specification

class JsoupBrowserSpec extends Specification with TestServer {

  def wrapHtml(str: String) = s"<html><body>$str</body></html>"

  lazy val testService = HttpService {
    case req @ GET -> Root / "agent" =>
      val userAgent = req.headers.get("User-Agent".ci).fold("")(_.value)
      Ok(wrapHtml(userAgent))
  }

  "A JsoupBrowser" should {

    "execute requests with the specified user agent" in {
      val browser = new JsoupBrowser("test-agent")

      val doc = browser.get(s"http://localhost:$testServerPort/agent")
      doc.body.text mustEqual "test-agent"
    }
  }
}
