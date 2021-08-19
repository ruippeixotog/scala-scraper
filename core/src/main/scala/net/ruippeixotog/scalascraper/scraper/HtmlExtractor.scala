package net.ruippeixotog.scalascraper.scraper

import scalaz.Monad

import net.ruippeixotog.scalascraper.model.{Element, ElementQuery}

/** An object able to extract content from [[net.ruippeixotog.scalascraper.model.ElementQuery]] instances.
  *
  * @tparam E
  *   the type of the elements needed by this `HtmlExtractor`
  * @tparam A
  *   the type of the extracted content
  */
trait HtmlExtractor[-E <: Element, +A] extends (ElementQuery[E] => A) {

  /** Extracts content from an `ElementQuery`.
    *
    * @param q
    *   the element query from which content is to be extracted
    * @return
    *   the extracted content.
    */
  def extract(q: ElementQuery[E]): A

  @inline final def apply(q: ElementQuery[E]) = extract(q)

  /** Maps a function over the extracted content of this extractor.
    *
    * @param f
    *   the function to map over this extractor
    * @tparam B
    *   the output type of the function
    * @return
    *   an `HtmlExtractor` returning the contents extracted by this extractor mapped by `f`.
    */
  def map[B](f: A => B): HtmlExtractor[E, B] =
    HtmlExtractor { q => f(extract(q)) }

  /** Applies a CSS query to `ElementQuery` inputs before passing them to this extractor.
    *
    * @param cssQuery
    *   the CSS query to apply to input `ElementQuery` instances
    * @return
    *   an `HtmlExtractor` returning the contents extracted by this extractor after `cssQuery` is applied to the input
    *   queries.
    */
  def mapQuery(cssQuery: String): HtmlExtractor[E, A] =
    HtmlExtractor { q => extract(q.select(cssQuery)) }
}

/** The companion object for `HtmlExtractor`, containing methods for creating new extractors.
  */
object HtmlExtractor extends HtmlExtractorInstances {

  /** Creates a new `HtmlExtractor` from a function.
    *
    * @param f
    *   the function used to extract content from an `ElementQuery`
    * @tparam E
    *   the type of the elements needed by the `HtmlExtractor`
    * @tparam A
    *   the type of the extracted content
    * @return
    *   a new `HtmlExtractor` that extracts content using `f`.
    */
  def apply[E <: Element, A](f: ElementQuery[E] => A): HtmlExtractor[E, A] =
    new HtmlExtractor[E, A] {
      def extract(q: ElementQuery[E]): A = f(q)
    }

  /** Creates a new `HtmlExtractor` that extracts the elements of the input that match a CSS query.
    *
    * @param cssQuery
    *   the CSS query to apply
    * @tparam E
    *   the type of the elements needed by the `HtmlExtractor`
    * @return
    *   a new `HtmlExtractor` that extracts the elements of the input that match a CSS query.
    */
  def forQuery[E <: Element](cssQuery: String): HtmlExtractor[E, ElementQuery[E]] =
    HtmlExtractor(_.select(cssQuery))
}

trait HtmlExtractorInstances {

  implicit def extractorMonad[E <: Element] =
    new Monad[({ type t[A] = HtmlExtractor[E, A] })#t] {
      def point[A](a: => A) =
        new HtmlExtractor[E, A] {
          def extract(q: ElementQuery[E]) = a
        }

      def bind[A, B](fa: HtmlExtractor[E, A])(f: A => HtmlExtractor[E, B]) =
        new HtmlExtractor[E, B] {
          def extract(q: ElementQuery[E]) = f(fa.extract(q)).extract(q)
        }

      override def map[A, B](fa: HtmlExtractor[E, A])(f: A => B) = fa.map(f)
    }
}

@deprecated("Use HtmlExtractor constructor methods followed by map and mapQuery", "2.0.0")
case class SimpleExtractor[-E <: Element, C, +A] @deprecated(
  "Use `contentExtractor.mapQuery(cssQuery).map(contentParser)` instead",
  "2.0.0"
) (cssQuery: String, contentExtractor: ElementQuery[E] => C, contentParser: C => A)
    extends HtmlExtractor[E, A] {

  def extract(q: ElementQuery[E]) = contentParser(contentExtractor(q.select(cssQuery)))

  def withQuery(cssQuery: String) = copy(cssQuery = cssQuery)

  def extractWith[C2](contentExtractor: ElementQuery[Element] => C) = copy(contentExtractor = contentExtractor)

  def extractWith[C2, A2](contentExtractor: ElementQuery[Element] => C2, contentParser: C2 => A2) =
    copy(contentExtractor = contentExtractor, contentParser = contentParser)

  def parseWith[A2](contentExtractor: C => A2) = copy(contentParser = contentParser)
}

object SimpleExtractor {

  @deprecated("Use `HtmlExtractor(cssQuery)` instead", "2.0.0")
  def apply(cssQuery: String): SimpleExtractor[Element, Iterable[String], Iterable[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs)

  @deprecated("Use `contentExtractor.mapQuery(cssQuery)` instead", "2.0.0")
  def apply[E <: Element, C](cssQuery: String, contentExtractor: ElementQuery[E] => C): SimpleExtractor[E, C, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs)
}
