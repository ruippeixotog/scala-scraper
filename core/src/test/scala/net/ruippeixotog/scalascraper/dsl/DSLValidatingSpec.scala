package net.ruippeixotog.scalascraper.dsl

import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.browser._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{text => stext, _}
import net.ruippeixotog.scalascraper.scraper.HtmlValidator._

class DSLValidatingSpec extends Specification with BrowserHelper {

  "The scraping DSL" should {

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      val doc = browser.parseResource("/test2.html")

      "allow validating content based on a validator" in {
        doc >/~ validator("#content section")(_.size == 3) must beRight(doc)
        doc >/~ validator("#content section")(_.size == 4) must beLeft(())
      }

      "allow identifying and possibly returning an error status in a validation" in {
        val succ = validator(stext("#menu .active"))(_ == "Section 3")
        val error1 = validator(stext("#menu .active"), "Shouldn't be in Section 2")(_ == "Section 2")
        val error2 = validator(stext("title"), "Not in text page")(_ != "Test page")

        doc >/~ (succ, error1) must beLeft("Shouldn't be in Section 2")
        doc >/~ (succ, error1, "Unknown") must beLeft("Shouldn't be in Section 2")
        doc >/~ (succ, error2) must throwA[ValidationException]
        doc >/~ (succ, error2, "Unknown") must beLeft("Unknown")
      }

      "allow returning one of multiple possible error statuses in a validation" in {
        val succ = validator(stext("#menu .active"))(_ == "Section 3")
        val errors = List(
          validator(attr("charset")("meta"), "Encoding isn't UTF-8")(_ != "utf-8"),
          validator(stext("title"), "Not in text page")(_ != "Test page"),
          validator(stext("#menu .active"), "Shouldn't be in Section 2")(_ == "Section 2")
        )

        doc >/~ (succ, errors) must beLeft("Shouldn't be in Section 2")
        doc >/~ (succ, errors, "Unknown") must beLeft("Shouldn't be in Section 2")
        doc >/~ (succ, errors.dropRight(1), "Unknown") must beLeft("Unknown")

        doc errorIf errors must beLeft("Shouldn't be in Section 2")
      }

      "allow extracting content before and after validating it" in {
        val v = validator("#content section")(_.size == 3)

        doc >> element("body") >/~ v must beRight(doc.body)
        doc >> element("#myform") >/~ v must beLeft(())
        doc >/~ v >> element("body") must beRight(doc.body)
        doc >/~ v >> element("#myform") must beRight(doc.root.select("#myform").head)
      }

      "provide match-all and match-nothing validators" in {
        doc >/~ matchAll must beRight(doc)
        doc.root.select("head") >/~ matchAll must beRight(doc.root.select("head"))
        doc.root.select("legs") >/~ matchAll must beRight(doc.root.select("legs"))

        doc errorIf matchAll(42) must beLeft(42)
        doc.root.select("head") errorIf matchAll("42") must beLeft("42")
        doc.root.select("legs") errorIf matchAll(42.0) must beLeft(42.0)

        doc >/~ matchNothing(42) must beLeft(())
        doc.root.select("head") >/~ matchNothing("42") must beLeft(())
        doc.root.select("legs") >/~ matchNothing(42.0) must beLeft(())

        doc errorIf matchNothing must beRight(doc)
        doc.root.select("head") errorIf matchNothing must beRight(doc.root.select("head"))
        doc.root.select("legs") errorIf matchNothing must beRight(doc.root.select("legs"))
      }
    }
  }
}
