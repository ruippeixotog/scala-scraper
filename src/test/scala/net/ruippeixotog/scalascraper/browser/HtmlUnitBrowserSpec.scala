package net.ruippeixotog.scalascraper.browser

import java.io.File

import scala.io.Source

import org.http4s.HttpService
import org.http4s.dsl._
import org.specs2.mutable.Specification

class HtmlUnitBrowserSpec extends Specification with TestServer {

  def wrapHtml(str: String) = s"<html><body>$str</body></html>"

  def serveResource(name: String) = Ok(Source.fromFile(
    getClass.getClassLoader.getResource(name).toURI).mkString)

  lazy val testService = HttpService {
    case GET -> Root / "jsredirect" => serveResource("testjs2.1.html")
    case GET -> Root / "jsredirected" => serveResource("testjs2.2.html")
  }

  "A JsoupBrowser" should {

    "execute JavaScript in HTML pages" in {
      val file = new File(getClass.getClassLoader.getResource("testjs.html").toURI)
      val doc = new HtmlUnitBrowser().parseFile(file)

      doc.root.select("#t").head.text mustEqual "Before"
      doc.root.select("#t").head.text must beEqualTo("After").eventually
    }

    "support JavaScript redirections" in {
      val doc = new HtmlUnitBrowser().get(s"http://localhost:$testServerPort/jsredirect")

      doc.location mustEqual s"http://localhost:$testServerPort/jsredirect"
      doc.root.select("#t").head.text mustEqual "First"

      doc.root.select("#t").head.text must beEqualTo("Second").eventually
      doc.location must beEqualTo(s"http://localhost:$testServerPort/jsredirected").eventually
      doc.root.select("#t").head.text mustEqual "Second"
    }
  }
}
