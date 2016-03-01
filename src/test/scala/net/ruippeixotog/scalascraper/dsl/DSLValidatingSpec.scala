package net.ruippeixotog.scalascraper.dsl

import java.io.File
import net.ruippeixotog.scalascraper.browser._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{ text => stext, _ }
import net.ruippeixotog.scalascraper.scraper.HtmlValidator._
import net.ruippeixotog.scalascraper.util.Validated._
import org.specs2.mutable.Specification

class DSLValidatingSpec extends Specification with BrowserHelper {

  "The scraping DSL" should {

    val file = new File(getClass.getClassLoader.getResource("test2.html").toURI)

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      val doc = browser.parseFile(file)

      "allow validating content based on a validator" in {
        doc ~/~ validator("#content section")(_.size == 3) mustEqual VSuccess(doc)
        doc ~/~ validator("#content section")(_.size == 4) mustEqual VFailure(())
      }

      "allow identifying and possibly returning an error status in a validation" in {
        val succ = validator(stext("#menu .active"))(_ == "Section 3")
        val error1 = validator(stext("#menu .active"), "Shouldn't be in Section 2")(_ == "Section 2")
        val error2 = validator(stext("title"), "Not in text page")(_ != "Test page")

        doc ~/~ (succ, error1) mustEqual VFailure("Shouldn't be in Section 2")
        doc ~/~ (succ, error1, "Unknown") mustEqual VFailure("Shouldn't be in Section 2")
        doc ~/~ (succ, error2) must throwA[ValidationException]
        doc ~/~ (succ, error2, "Unknown") mustEqual VFailure("Unknown")
      }

      "allow returning one of multiple possible error statuses in a validation" in {
        val succ = validator(stext("#menu .active"))(_ == "Section 3")
        val errors = List(
          validator(attr("charset")("meta")) {
            _ != "utf-8"
          } withResult "Encoding isn't UTF-8",
          validator(stext("title")) {
            _ != "Test page"
          } withResult "Not in text page",
          validator(stext("#menu .active")) {
            _ == "Section 2"
          } withResult "Shouldn't be in Section 2")

        doc ~/~ (succ, errors) mustEqual VFailure("Shouldn't be in Section 2")
        doc ~/~ (succ, errors, "Unknown") mustEqual VFailure("Shouldn't be in Section 2")
        doc ~/~ (succ, errors.dropRight(1), "Unknown") mustEqual VFailure("Unknown")

        doc errorIf errors mustEqual VFailure("Shouldn't be in Section 2")
      }

      "allow extracting content before and after validating it" in {
        val v = validator("#content section")(_.size == 3)

        doc >> element("body") ~/~ v mustEqual VSuccess(doc.body)
        doc >> element("#myform") ~/~ v mustEqual VFailure(())
        doc ~/~ v >> element("body") mustEqual VSuccess(doc.body)
        doc ~/~ v >> element("#myform") mustEqual VSuccess(doc.root.select("#myform").head)
      }

      "provide match-all and match-nothing validators" in {
        doc ~/~ matchAll mustEqual VSuccess(doc)
        doc.select("head") ~/~ matchAll mustEqual VSuccess(doc.select("head"))
        doc.select("body") ~/~ matchAll mustEqual VSuccess(doc.select("body"))
        doc.select("legs") ~/~ matchAll mustEqual VSuccess(doc.select("legs"))

        doc ~/~ matchNothing mustEqual VFailure(())
        doc.select("head") ~/~ matchNothing mustEqual VFailure(())
        doc.select("body") ~/~ matchNothing mustEqual VFailure(())
        doc.select("legs") ~/~ matchNothing mustEqual VFailure(())
      }
    }
  }
}
