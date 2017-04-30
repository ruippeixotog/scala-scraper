package net.ruippeixotog.scalascraper.config

import scala.collection.JavaConverters._

import com.typesafe.config.Config

import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{ allText, attr }
import net.ruippeixotog.scalascraper.scraper.ContentParsers.{ asDate, asIs, regexMatch }
import net.ruippeixotog.scalascraper.scraper.SimpleExtractor

object ConfigHtmlExtractor {

  def apply[A](conf: Config) = {
    val cssQuery = conf.getString("query")

    val contentExtractor =
      if (conf.hasPath("attr")) attr(conf.getString("attr")) else allText

    val contentParser =
      if (conf.hasPath("date-format"))
        asDate(conf.getString("date-format"))
      else if (conf.hasPath("date-formats"))
        asDate(conf.getStringList("date-formats").asScala: _*)
      else if (conf.hasPath("regex-format"))
        regexMatch(conf.getString("regex-format"))
      else
        asIs[String]

    SimpleExtractor(cssQuery, contentExtractor, contentParser.andThen(_.asInstanceOf[A]))
  }
}
