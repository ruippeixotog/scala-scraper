package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.ElementQuery
import net.ruippeixotog.scalascraper.scraper._

object DSL extends ImplicitConversions with ScrapingOps with ConfigLoadingHelpers {

  def extractor(cssQuery: String): HtmlExtractor[Iterable[String]] = SimpleExtractor(cssQuery)

  def extractor[C](cssQuery: String, contentExtractor: ElementQuery => C): HtmlExtractor[C] =
    SimpleExtractor(cssQuery, contentExtractor)

  def extractor[C, A](cssQuery: String, contentExtractor: ElementQuery => C, contentParser: C => A): HtmlExtractor[A] =
    SimpleExtractor(cssQuery, contentExtractor, contentParser)

  val validator = SimpleValidator

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
