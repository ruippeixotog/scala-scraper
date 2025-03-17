package net.ruippeixotog.scalascraper.config.dsl

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.config.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL._

class ConfigLoadingHelpersSpec extends Specification {

  "The scraping DSL" should {

    val doc = JsoupBrowser().parseResource("/test2.html")
    val docEmpty = JsoupBrowser().parseResource("/test.html")

    "be able to create extractors from configuration files" in {
      val conf = ConfigFactory.load.getConfig("test-extractors")

      doc >> extractorAt[String](conf, "simple") mustEqual "No copyright 2014"
      doc >> extractorAt[String](conf, "attr") mustEqual "utf-8"
      doc >> extractorAt[DateTime](conf, "date-format") mustEqual "2014-10-26".toDateTime
      doc >> extractorAt[DateTime](conf, "date-formats") mustEqual "2014-10-26".toDateTime
      doc >> extractorAt[String](conf, "regex-format") mustEqual "Section 2 h3"
    }

    "be able to create validators from configuration files" in {
      val conf = ConfigFactory.load.getConfig("test-validators")

      doc >/~ validatorAt(conf, "simple") must beRight(doc)
      docEmpty >/~ validatorAt(conf, "simple") must beLeft(())

      doc >/~ validatorAt(conf, "attr") must beLeft(())
      docEmpty >/~ validatorAt(conf, "attr") must beLeft(())

      doc >/~ validatorAt(conf, "with-result") must beRight(doc)
      doc `errorIf` validatorAt[Int](conf, "with-result") must beLeft(5)
      doc `errorIf` validatorAt[String](conf, "with-result") must beLeft("5")
      doc `errorIf` validatorAt[Boolean](conf, "with-result") must throwAn[Exception]

      doc >/~ validatorAt(conf, "exists") must beRight(doc)
      docEmpty >/~ validatorAt(conf, "exists") must beRight(docEmpty)
      doc `errorIf` validatorAt[Double](conf, "exists") must beLeft(0.25)

      doc >/~ validatorAt(conf, "exists2") must beLeft(())
      docEmpty >/~ validatorAt(conf, "exists2") must beRight(docEmpty)
      doc `errorIf` validatorAt[Double](conf, "exists2") must beRight(doc)

      doc >/~ validatorAt(conf, "inverted") must beRight(doc)
      doc `errorIf` validatorAt[LocalDate](conf, "inverted") must beLeft("2013-03-03".toLocalDate)
    }
  }
}
