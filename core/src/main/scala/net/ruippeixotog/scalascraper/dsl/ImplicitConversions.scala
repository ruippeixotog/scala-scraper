package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import net.ruippeixotog.scalascraper.scraper.{ HtmlExtractor, PolyHtmlExtractor }

trait ImplicitConversions {

  implicit def cssQueryAsExtractor[E <: Element](cssQuery: String): HtmlExtractor[E, ElementQuery[E]] =
    HtmlExtractor.forQuery(cssQuery)

  implicit def cssQueryAsPolyExtractor(cssQuery: String): PolyHtmlExtractor.Aux[pElements.Out] =
    pElements.mapQuery(cssQuery)

  implicit class RichHtmlExtractor[E <: Element, C](extractor: HtmlExtractor[E, C]) {
    def apply(cssQuery: String) = extractor.mapQuery(cssQuery)
  }
}

object ImplicitConversions extends ImplicitConversions
