package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.Element

trait PolyHtmlExtractor { outer =>
  type Out[E <: Element]

  def apply[E <: Element]: HtmlExtractor[E, Out[E]]

  def mapQuery(cssQuery: String): PolyHtmlExtractor.Aux[Out] = new PolyHtmlExtractor {
    type Out[E <: Element] = outer.Out[E]
    def apply[E <: Element] = outer[E].mapQuery(cssQuery)
  }

  // TODO try to find a way to make this a DSL extension method. If that really can't be done, consider deprecating this
  def apply(cssQuery: String): PolyHtmlExtractor.Aux[Out] = mapQuery(cssQuery)
}

object PolyHtmlExtractor {
  type Aux[Out0[E <: Element]] = PolyHtmlExtractor { type Out[E <: Element] = Out0[E] }

  implicit def polyHtmlExtractorAsExtractor[E <: Element](
    polyExtractor: PolyHtmlExtractor): HtmlExtractor[E, polyExtractor.Out[E]] = polyExtractor[E]
}
