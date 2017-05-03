package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

trait ImplicitConversions {

  implicit def cssQueryAsExtractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    HtmlExtractor.forQuery(cssQuery)

  implicit class RichHtmlExtractor[E <: Element, C](contentExtractor: HtmlExtractor[E, C]) {
    def apply(cssQuery: String) = contentExtractor.mapQuery(cssQuery)
  }
}

object ImplicitConversions extends ImplicitConversions
