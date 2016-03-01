package net.ruippeixotog.scalascraper.dsl

import java.io.File

import com.github.nscala_time.time.Imports._
import net.ruippeixotog.scalascraper.browser._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.ElementQuery
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{ text => stext, _ }
import net.ruippeixotog.scalascraper.scraper.ContentParsers._
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import org.specs2.mutable.Specification

class DSLExtractingSpec extends Specification with BrowserHelper {

  "The scraping DSL" should {

    val file = new File(getClass.getClassLoader.getResource("test2.html").toURI)

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      val doc = browser.parseFile(file)

      "allow extracting the first element matched by a CSS query" in {
        (doc >> element("#content > section > h3")).tagName mustEqual "h3"
        (doc >> element("#content > section > h3")).text mustEqual "Section 1 h3"

        (doc >> element(".active")).text mustEqual "Section 2"
        (doc >> element(".active")).attr("class") mustEqual "active"

        (doc >> element("p")).text mustEqual "Some text for testing"
      }

      "allow extracting the content of the first element matched by a CSS query" in {
        doc >> stext("#content > section > h3") mustEqual "Section 1 h3"
        doc >> stext(".active") mustEqual "Section 2"
        doc >> stext("p") mustEqual "Some text for testing"
      }

      "allow extracting all elements matched by a CSS query" in {
        (doc >> elements("#content > section")).map(_.tagName) mustEqual Seq("section", "section", "section")

        (doc >> elements("#content > section > h3")).map(_.text) mustEqual
          Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")

        (doc >> elements(".active")).map(_.text) mustEqual Seq("Section 2")
        (doc >> elements("p")).map(_.text) mustEqual Seq("Some text for testing", "More text for testing")
      }

      "allow extracting the content of all elements matched by a CSS query" in {
        (doc >> "#content > section").size mustEqual 3
        doc >> "#content > section > h3" mustEqual Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")
        doc >> "#menu .active" mustEqual Seq("Section 2")
      }

      "allow extracting an attribute of the first element matched by a CSS query" in {
        doc >> extractor(".active", attr("class")) mustEqual "active"
      }

      "allow extracting an attribute of all elements matched by a CSS query" in {
        doc >> extractor("#menu a", attrs("href")) mustEqual Seq("#home", "#section1", "#section3")
      }

      "allow extracting form data from a HTML form" in {
        doc >> formData("#myform") mustEqual Map("a" -> "4", "b" -> "user", "c" -> "data")
        doc >> formDataAndAction("#myform") mustEqual (Map("a" -> "4", "b" -> "user", "c" -> "data"), "submit.html")
      }

      "support immediate parsing of numbers after extraction" in {
        doc >> extractor("#rating", stext, asDouble) mustEqual 4.5
        doc >> extractor("#mytable td", texts, seq(asInt)) mustEqual Seq(3, 15, 1)
      }

      "allow immediate parsing of dates after extraction" in {
        (doc >> extractor("#date", stext, asDate("yyyy-MM-dd"))).toLocalDate mustEqual "2014-10-26".toLocalDate
      }

      "allow immediate parsing with a regex after extraction" in {
        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) h3")) mustEqual "Section 1 h3"
        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) h3").captured) mustEqual "1"

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) h3")) mustEqual
          Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) h3").captured) mustEqual
          Seq("1", "2", "3")

        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) (h.)").allCaptured) mustEqual
          List("1", "h3")

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) (h.)").allCaptured) mustEqual
          Seq(List("1", "h3"), List("2", "h3"), List("3", "h3"))
      }

      "allow extracting and parsing custom content" in {
        case class MyPage(title: String, date: LocalDate, section: String)

        val myExtractor = new HtmlExtractor[MyPage] {
          def extract(doc: ElementQuery) = MyPage(
            doc >> stext("title"),
            (doc >> extractor("#date", stext, asDate("yyyy-MM-dd"))).toLocalDate,
            doc >> stext("#menu .active"))
        }

        doc >> myExtractor mustEqual MyPage("Test page", "2014-10-26".toLocalDate, "Section 2")
      }
    }
  }
}
