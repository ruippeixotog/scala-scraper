package net.ruippeixotog.scalascraper.dsl

import java.io.File

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{text => stext, _}
import org.jsoup.nodes.Document
import org.specs2.mutable.Specification

class DSLSpec extends Specification {

  val file = new File(getClass.getClassLoader.getResource("test2.html").toURI)
  val doc = new Browser().parseFile(file)

  "The scraping DSL" should {

    "allow trying to extract content which may or may not exist" in {
      doc >?> stext("title") mustEqual Some("Test page")
      doc >?> stext("unknown") mustEqual None
    }

    "support using two extractors at once" in {
      doc >> (stext("title"), stext("#menu .active")) mustEqual ("Test page", "Section 2")
    }

    "support extracting content inside Options and Lists" in {
      Option(doc) >> stext("title") mustEqual Some("Test page")
      Option.empty[Document] >> stext("title") mustEqual None

      List(doc, doc) >> stext("title") mustEqual List("Test page", "Test page")
      List.empty[Document] >> stext("title") mustEqual Nil
    }

    "support chaining element extractors" in {
      doc >> element("#menu") >> stext(".active") mustEqual "Section 2"
      doc >?> element("#menu") >> stext(".active") mustEqual Some("Section 2")
      doc >?> element("#menu") >?> stext(".active") mustEqual Some(Some("Section 2"))
      doc >?> element("unknown") >?> stext(".active") mustEqual None
      doc >> elementList("#menu span") >?> stext("a") mustEqual
        Seq(Some("Home"), Some("Section 1"), None, Some("Section 3"))
    }
  }
}
