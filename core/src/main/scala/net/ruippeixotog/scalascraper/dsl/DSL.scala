package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }
import net.ruippeixotog.scalascraper.scraper._

object DSL extends ImplicitConversions with ScrapingOps {

  def extractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] = SimpleExtractor(cssQuery)

  def extractor[E <: Element, C](cssQuery: String, contentExtractor: ElementQuery[E] => C): HtmlExtractor[E, C] =
    SimpleExtractor(cssQuery, contentExtractor)

  def extractor[E <: Element, C, A](cssQuery: String, contentExtractor: ElementQuery[E] => C, contentParser: C => A): HtmlExtractor[E, A] =
    SimpleExtractor(cssQuery, contentExtractor, contentParser)

  val validator = SimpleValidator

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
