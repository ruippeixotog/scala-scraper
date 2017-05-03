package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

trait ImplicitConversions {

  implicit def cssQueryAsExtractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    HtmlExtractor(cssQuery)

  implicit def contentExtractorAsExtractor[E <: Element, C](contentExtractor: ElementQuery[E] => C): String => HtmlExtractor[E, C] = {
    cssQuery: String => HtmlExtractor(cssQuery, contentExtractor)
  }
}

object ImplicitConversions extends ImplicitConversions
