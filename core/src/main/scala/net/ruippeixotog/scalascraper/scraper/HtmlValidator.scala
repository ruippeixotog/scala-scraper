package net.ruippeixotog.scalascraper.scraper

import scala.util.Try

import net.ruippeixotog.scalascraper.model.{Element, ElementQuery}

trait HtmlValidator[-E <: Element, +R] { outer =>
  def matches(doc: ElementQuery[E]): Boolean
  def result: Option[R]

  def withResult[R2](res: R2): HtmlValidator[E, R2] =
    HtmlValidator(ContentExtractors.pElements[E], res)(matches)

  def withoutResult: HtmlValidator[E, Nothing] =
    HtmlValidator(ContentExtractors.pElements[E])(matches)
}

object HtmlValidator {

  def apply[E <: Element, A, R](extractor: HtmlExtractor[E, A])(matcher: A => Boolean): HtmlValidator[E, R] =
    new HtmlValidatorImpl[E, A, R](extractor, matcher)

  def apply[E <: Element, A, R](extractor: HtmlExtractor[E, A], result: R)(matcher: A => Boolean): HtmlValidator[E, R] =
    new HtmlValidatorImpl[E, A, R](extractor, matcher, Some(result))

  def apply[R](
      polyExtractor: PolyHtmlExtractor
  )(matcher: polyExtractor.Out[Element] => Boolean): HtmlValidator[Element, R] =
    new HtmlValidatorImpl[Element, polyExtractor.Out[Element], R](polyExtractor[Element], matcher)

  def apply[R](polyExtractor: PolyHtmlExtractor, result: R)(
      matcher: polyExtractor.Out[Element] => Boolean
  ): HtmlValidator[Element, R] =
    new HtmlValidatorImpl[Element, polyExtractor.Out[Element], R](polyExtractor[Element], matcher, Some(result))

  val matchAll = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = true
    def result: Option[Nothing] = None
  }

  val matchNothing = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = false
    def result: Option[Nothing] = None
  }

  def matchAll[R](res: R) =
    new HtmlValidator[Element, R] {
      def matches(doc: ElementQuery[Element]) = true
      def result: Option[R] = Some(res)
    }

  def matchNothing[R](res: R) =
    new HtmlValidator[Element, R] {
      def matches(doc: ElementQuery[Element]) = false
      def result: Option[R] = Some(res)
    }

  private[this] class HtmlValidatorImpl[-E <: Element, A, +R](
      htmlExtractor: HtmlExtractor[E, A],
      matcher: A => Boolean,
      val result: Option[R] = None
  ) extends HtmlValidator[E, R] {

    def matches(doc: ElementQuery[E]) = Try(htmlExtractor.extract(doc)).map(matcher).getOrElse(false)
  }
}
