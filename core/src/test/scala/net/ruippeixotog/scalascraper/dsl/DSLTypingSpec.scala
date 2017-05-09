package net.ruippeixotog.scalascraper.dsl

import org.http4s.HttpService
import org.http4s.dsl._
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.browser._
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser.HtmlUnitDocument
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{ text => stext, _ }

class DSLTypingSpec extends Specification with TestServer {

  lazy val testService = HttpService {
    case GET -> Root / "clickable" => serveText("""<a href="/clicked"></a>""")
    case GET -> Root / "clicked" => serveText("clicked")
  }

  "The scraping DSL" should {

    "using a typed JsoupBrowser" in {

      lazy val doc = JsoupBrowser.typed().parseResource("/test2.html")

      "allow extracting the underlying jsoup Element instances" in {
        doc >> "#menu" >> stext(".active") mustEqual "Section 2"
        (doc >> "#menu" >> pElement(".active")).underlying.removeClass("active")
        (doc >> "#menu" >> pElement("a[href=#section1]")).underlying.addClass("active")
        doc >> "#menu" >> stext(".active") mustEqual "Section 1"

        doc >> "#mytable td" foreach { e => e.underlying.text("42") }
        doc >> pElementList("#mytable td") map { _.text } mustEqual List("42", "42", "42", "42")
        doc >> extractor("#mytable td", pElementList) map { _.text } mustEqual List("42", "42", "42", "42")
      }
    }

    "using a typed HtmlUnitBrowser" in {

      lazy val doc = HtmlUnitBrowser.typed().parseResource("/test2.html")
      lazy val doc2 = HtmlUnitBrowser.typed().get(s"http://localhost:$testServerPort/clickable")

      "allow extracting the underlying HtmlUnit DomElement instances" in {
        doc >> "#menu" >> stext(".active") mustEqual "Section 2"
        (doc >> "#menu" >> pElement(".active")).underlying.removeAttribute("class")
        (doc >> "#menu" >> pElement("span:nth-child(2) a")).underlying.setAttribute("class", "active")
        doc >> "#menu" >> stext(".active") mustEqual "Section 1"

        doc >> "#mytable td" foreach { e => e.underlying.setTextContent("42") }
        doc >> pElementList("#mytable td") map { _.text } mustEqual List("42", "42", "42", "42")
        doc >> extractor("#mytable td", pElementList) map { _.text } mustEqual List("42", "42", "42", "42")

        (doc2 >> pElement("a")).underlying.click()
        doc2 >> stext mustEqual "clicked"
      }
    }
  }
}
