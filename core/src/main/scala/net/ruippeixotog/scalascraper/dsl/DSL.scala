package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper._

object DSL extends ImplicitConversions with ScrapingOps {

  def extractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    HtmlExtractor.forQuery(cssQuery)

  def extractor[E <: Element, C](cssQuery: String, contentExtractor: HtmlExtractor[E, C]): HtmlExtractor[E, C] =
    contentExtractor.mapQuery(cssQuery)

  def extractor[E <: Element, C, A](
    cssQuery: String, contentExtractor: HtmlExtractor[E, C], contentParser: C => A): HtmlExtractor[E, A] = {

    contentExtractor.mapQuery(cssQuery).map(contentParser)
  }

  val validator = HtmlValidator

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
