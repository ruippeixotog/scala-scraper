package net.ruippeixotog.scalascraper.dsl

import com.github.nscala_time.time.Imports._

import net.ruippeixotog.scalascraper.browser._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{ text => stext, _ }
import net.ruippeixotog.scalascraper.scraper.ContentParsers._
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import org.specs2.mutable.Specification

class DSLExtractingSpec extends Specification with BrowserHelper {

  "The scraping DSL" should {

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      val doc = browser.parseResource("/test2.html")

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
        doc >> stext("#footer") mustEqual "No copyright 2014"
      }

      "allow extracting all elements matched by a CSS query" in {
        (doc >> elements("#content > section")).map(_.tagName) mustEqual Seq("section", "section", "section")

        (doc >> elements("#content > section > h3")).map(_.text) mustEqual
          Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")

        (doc >> elements(".active")).map(_.text) mustEqual Seq("Section 2")
        (doc >> elements("p")).map(_.text) mustEqual Seq("Some text for testing", "More text for testing")

        (doc >> "#content > section").map(_.tagName) mustEqual Seq("section", "section", "section")
        (doc >> "#content > section > h3").map(_.text) mustEqual Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")
        (doc >> "#menu .active").map(_.text) mustEqual Seq("Section 2")
      }

      "allow extracting the content of all elements matched by a CSS query" in {
        (doc >> texts("#content > section")).size mustEqual 3
        doc >> texts("#content > section > h3") mustEqual Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")
        doc >> texts("#menu .active") mustEqual Seq("Section 2")
      }

      "allow extracting an attribute of the first element matched by a CSS query" in {
        doc >> extractor(".active", attr("class")) mustEqual "active"
        doc >> extractor(".active", attr("data-a")) must throwA[NoSuchElementException]
      }

      "allow extracting an attribute of all elements matched by a CSS query" in {
        doc >> extractor("#menu a", attrs("href")) mustEqual Seq("#home", "#section1", "#section3")
      }

      "allow extracting form data from a HTML form" in {
        doc >> formData("#myform") mustEqual Map("name" -> "John", "address" -> "")
        doc >> formDataAndAction("#myform") mustEqual (Map("name" -> "John", "address" -> ""), "submit.html")
      }

      "support immediate parsing of numbers after extraction" in {
        doc >> extractor("#rating", stext, asDouble) mustEqual 4.5
        doc >> extractor("#mytable td", texts, seq(asInt)).map(_.toSeq) mustEqual Seq(3, 15, 15, 1)
      }

      "allow immediate parsing of dates after extraction" in {
        doc >> extractor("#date", stext, asLocalDate("yyyy-MM-dd")) mustEqual "2014-10-26".toLocalDate
        doc >> extractor("#date", stext, asLocalDate("yyyy-MMM-dd")) must throwAn[Exception]
        doc >> extractor("#date", stext, asLocalDate("yyyy-MMM-dd", "yyyy-MM-dd")) mustEqual "2014-10-26".toLocalDate

        doc >> extractor("#datefull", stext, asDateTime("yyyy-MM-dd'T'HH:mm:ssZ")) mustEqual "2014-10-26T12:30:05Z".toDateTime
        doc >> extractor("#datefull", stext, asDateTime("yyyy-MMM-dd'T'HH:mm:ssZ")) must throwAn[Exception]
        doc >> extractor("#datefull", stext, asDateTime("yyyy-MMM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ssZ")) mustEqual
          "2014-10-26T12:30:05Z".toDateTime

        val customTz = DateTimeZone.forOffsetHours(4)

        // without any indication of time zone, datetimes are parsed using the local time zone
        doc >> extractor("#date", stext, asDateTime("yyyy-MM-dd")) mustEqual "2014-10-26".toDateTime

        // when a time zone for `asDateTime` is provided, it is assumed as the default time zone when no information is
        // avialable in the string to be parsed
        doc >> extractor("#date", stext, asDateTime("yyyy-MM-dd").withZone(customTz)) mustEqual
          "2014-10-26T00:00:00+04:00".toDateTime.withZone(customTz)

        // when there is time zone in the string to be parsed, the datetime is just changed to the provided time zone
        doc >> extractor("#datefull", stext, asDateTime("yyyy-MM-dd'T'HH:mm:ssZ").withZone(customTz)) mustEqual
          "2014-10-26T12:30:05Z".toDateTime.withZone(customTz)
      }

      "allow immediate parsing with a regex after extraction" in {
        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) h3")) mustEqual "Section 1 h3"
        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) h3".r)) mustEqual "Section 1 h3"
        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) h3").captured) mustEqual "1"

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) h3")).map(_.toSeq) mustEqual
          Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) h3".r)).map(_.toSeq) mustEqual
          Seq("Section 1 h3", "Section 2 h3", "Section 3 h3")

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) h3").captured).map(_.toSeq) mustEqual
          Seq("1", "2", "3")

        doc >> extractor("#content h3", allText, regexMatch("Section (\\d*) (h.)").allCaptured).map(_.toSeq) mustEqual
          Seq("1", "h3")

        doc >> extractor("#content h3", allText, regexMatches("Section (\\d*) (h.)").allCaptured).map(_.toSeq) mustEqual
          Seq(List("1", "h3"), List("2", "h3"), List("3", "h3"))
      }

      "allow extracting and parsing custom content" in {
        case class MyPage(title: String, date: LocalDate, section: String)

        val myExtractor = new HtmlExtractor[Element, MyPage] {
          def extract(doc: ElementQuery[Element]) = MyPage(
            doc >> stext("title"),
            doc >> extractor("#date", stext, asLocalDate("yyyy-MM-dd")),
            doc >> stext("#menu .active"))
        }

        doc >> myExtractor mustEqual MyPage("Test page", "2014-10-26".toLocalDate, "Section 2")
      }
    }
  }
}
