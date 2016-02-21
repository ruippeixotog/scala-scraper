package net.ruippeixotog.scalascraper.dsl

import java.io.File

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.util.Validated.{ VFailure, VSuccess }
import org.specs2.mutable.Specification

class ConfigLoadingHelpersSpec extends Specification {

  "The scraping DSL" should {

    val file = new File(getClass.getClassLoader.getResource("test2.html").toURI)
    val fileEmpty = new File(getClass.getClassLoader.getResource("test.html").toURI)
    val doc = JsoupBrowser().parseFile(file)
    val docEmpty = JsoupBrowser().parseFile(fileEmpty)

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

      doc ~/~ validatorAt(conf, "simple") mustEqual VSuccess(doc)
      docEmpty ~/~ validatorAt(conf, "simple") mustEqual VFailure(())

      doc ~/~ validatorAt(conf, "attr") mustEqual VFailure(())
      docEmpty ~/~ validatorAt(conf, "attr") mustEqual VFailure(())

      doc ~/~ validatorAt(conf, "with-result") mustEqual VSuccess(doc)
      doc errorIf validatorAt[Int](conf, "with-result") mustEqual VFailure(5)
      doc errorIf validatorAt[String](conf, "with-result") mustEqual VFailure("5")
      doc errorIf validatorAt[Boolean](conf, "with-result") must throwAn[Exception]

      doc ~/~ validatorAt(conf, "exists") mustEqual VSuccess(doc)
      docEmpty ~/~ validatorAt(conf, "exists") mustEqual VSuccess(docEmpty)
      doc errorIf validatorAt[Double](conf, "exists") mustEqual VFailure(0.25)

      doc ~/~ validatorAt(conf, "exists2") mustEqual VFailure(())
      docEmpty ~/~ validatorAt(conf, "exists2") mustEqual VSuccess(docEmpty)
      doc errorIf validatorAt[Double](conf, "exists2") mustEqual VSuccess(doc)

      doc ~/~ validatorAt(conf, "inverted") mustEqual VSuccess(doc)
      doc errorIf validatorAt[LocalDate](conf, "inverted") mustEqual VFailure("2013-03-03".toLocalDate)
    }
  }
}
