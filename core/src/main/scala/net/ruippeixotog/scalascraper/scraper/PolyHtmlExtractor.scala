package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.Element

/** An extractor like [[HtmlExtractor]] but whose extracted content type depends on the type of the input
  * [[net.ruippeixotog.scalascraper.model.Element]] s. A `PolyHtmlExtractor` supports application of CSS queries and can
  * be turned into a normal `HtmlExtractor` by calling its `apply[E]` method, fixing the type of the input `Element` as
  * `E`.
  */
trait PolyHtmlExtractor { outer =>

  /** The type of the extracted content as a function of the input elements.
    *
    * @tparam E
    *   the type of the input elements
    */
  type Out[E <: Element]

  /** Returns an `HtmlExtractor` obtained from this extractor by specifying the type of the input elements.
    *
    * @tparam E
    *   the type of the input elements
    * @return
    *   an `HtmlExtractor` obtained from this extractor by specifying the type of the input elements.
    */
  def apply[E <: Element]: HtmlExtractor[E, Out[E]]

  /** Applies a CSS query to `ElementQuery` inputs before passing them to this extractor.
    *
    * @param cssQuery
    *   the CSS query to apply to input `ElementQuery` instances
    * @return
    *   a `PolyHtmlExtractor` returning the contents extracted by this extractor after `cssQuery` is applied to the
    *   input queries.
    */
  def mapQuery(cssQuery: String): PolyHtmlExtractor.Aux[Out] =
    new PolyHtmlExtractor {
      type Out[E <: Element] = outer.Out[E]
      def apply[E <: Element] = outer[E].mapQuery(cssQuery)
    }

  /** Applies a CSS query to `ElementQuery` inputs before passing them to this extractor.
    *
    * @param cssQuery
    *   the CSS query to apply to input `ElementQuery` instances
    * @return
    *   a `PolyHtmlExtractor` returning the contents extracted by this extractor after `cssQuery` is applied to the
    *   input queries.
    */
  // TODO try to find a way to make this a DSL extension method. If that really can't be done, consider deprecating
  // this DSL construction
  def apply(cssQuery: String): PolyHtmlExtractor.Aux[Out] = mapQuery(cssQuery)
}

object PolyHtmlExtractor {
  type Aux[Out0[E <: Element]] = PolyHtmlExtractor { type Out[E <: Element] = Out0[E] }

  implicit def polyHtmlExtractorAsExtractor[E <: Element](
      polyExtractor: PolyHtmlExtractor
  ): HtmlExtractor[E, polyExtractor.Out[E]] = polyExtractor[E]
}
