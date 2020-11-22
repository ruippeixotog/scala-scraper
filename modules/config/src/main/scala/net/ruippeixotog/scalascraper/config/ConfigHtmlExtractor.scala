package net.ruippeixotog.scalascraper.config

import scala.collection.JavaConverters._

import com.typesafe.config.Config

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{allText, attr}
import net.ruippeixotog.scalascraper.scraper.ContentParsers.{asDateTime, asIs, regexMatch}
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

object ConfigHtmlExtractor {

  def apply[A](conf: Config): HtmlExtractor[Element, A] = {
    val cssQuery = conf.getString("query")

    val contentExtractor =
      if (conf.hasPath("attr")) attr(conf.getString("attr")) else allText

    val contentParser =
      if (conf.hasPath("date-format"))
        asDateTime(conf.getString("date-format"))
      else if (conf.hasPath("date-formats"))
        asDateTime(conf.getStringList("date-formats").asScala.toSeq: _*)
      else if (conf.hasPath("regex-format"))
        regexMatch(conf.getString("regex-format"))
      else
        asIs[String]

    contentExtractor.mapQuery(cssQuery).map(contentParser).map(_.asInstanceOf[A])
  }
}
