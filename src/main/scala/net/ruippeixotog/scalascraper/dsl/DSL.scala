package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper._
import org.jsoup.select.Elements

import scala.collection.convert.WrapAsScala

object DSL extends ImplicitConversions with ScrapingOps with ConfigLoadingHelpers with WrapAsScala {

  def extractor(cssQuery: String): HtmlExtractor[Seq[String]] = SimpleExtractor(cssQuery)

  def extractor[C](cssQuery: String, contentExtractor: Elements => C): HtmlExtractor[C] =
    SimpleExtractor(cssQuery, contentExtractor)

  def extractor[C, A](cssQuery: String, contentExtractor: Elements => C, contentParser: C => A): HtmlExtractor[A] =
    SimpleExtractor(cssQuery, contentExtractor, contentParser)

  val validator = SimpleValidator

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
