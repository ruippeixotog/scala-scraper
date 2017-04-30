package net.ruippeixotog.scalascraper.config

import scala.util.{ Failure, Success, Try }

import com.typesafe.config.Config

import net.ruippeixotog.scalascraper.config.util.ConfigReader
import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }
import net.ruippeixotog.scalascraper.scraper._

object ConfigHtmlValidator {

  def apply[R](conf: Config)(implicit reader: ConfigReader[R]): HtmlValidator[Element, R] = {
    val result = Try(reader.read(conf, "status"))

    if (conf.hasPath("exists")) {
      val extractor = HtmlExtractor(
        conf.getString("select.query"),
        ContentExtractors.elements, ContentParsers.asIs[ElementQuery[Element]])

      val matcher = conf.getBoolean("exists") == (_: Iterable[Element]).nonEmpty
      result match {
        case Success(res) => HtmlValidator(extractor, res)(matcher)
        case Failure(_) => HtmlValidator(extractor)(matcher)
      }

    } else {
      val extractor = ConfigHtmlExtractor[String](conf.getConfig("select"))
      val matchIfTrue = if (conf.hasPath("invert")) !conf.getBoolean("invert") else true
      val matcher = conf.getString("match").r.pattern.matcher(_: String).matches() == matchIfTrue
      result match {
        case Success(res) => HtmlValidator(extractor, res)(matcher)
        case Failure(_) => HtmlValidator(extractor)(matcher)
      }
    }
  }
}
