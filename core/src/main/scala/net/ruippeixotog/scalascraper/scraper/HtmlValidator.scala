package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.{ ElementQuery, Element }

import scala.util.Try

trait HtmlValidator[-E <: Element, +R] {
  def matches(doc: ElementQuery[E]): Boolean
  def result: Option[R]
}

object HtmlValidator {

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A])(matcher: A => Boolean): HtmlValidator[E, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher)

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A], result: R)(matcher: A => Boolean): HtmlValidator[E, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher, Some(result))

  def apply[R](polyExtractor: PolyHtmlExtractor)(matcher: polyExtractor.Out[Element] => Boolean): HtmlValidator[Element, R] =
    SimpleValidator[Element, polyExtractor.Out[Element], R](polyExtractor[Element], matcher)

  def apply[R](polyExtractor: PolyHtmlExtractor, result: R)(matcher: polyExtractor.Out[Element] => Boolean): HtmlValidator[Element, R] =
    SimpleValidator[Element, polyExtractor.Out[Element], R](polyExtractor[Element], matcher, Some(result))

  val matchAll = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = true
    def result = None
  }

  val matchNothing = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = false
    def result = None
  }

  def matchAll[R](res: R) = new HtmlValidator[Element, R] {
    def matches(doc: ElementQuery[Element]) = true
    def result = Some(res)
  }

  def matchNothing[R](res: R) = new HtmlValidator[Element, R] {
    def matches(doc: ElementQuery[Element]) = false
    def result = Some(res)
  }
}

@deprecated("SimpleValidator is deprecated. Use HtmlValidator.apply methods instead", "2.0.0")
case class SimpleValidator[-E <: Element, A, +R](
    htmlExtractor: HtmlExtractor[E, A],
    matcher: A => Boolean,
    result: Option[R] = None) extends HtmlValidator[E, R] {

  def matches(doc: ElementQuery[E]) = Try(htmlExtractor.extract(doc)).map(matcher).getOrElse(false)

  def withResult[R2](res: R2) = SimpleValidator(htmlExtractor, matcher, Some(res))
  def withoutResult = SimpleValidator(htmlExtractor, matcher, None)
}

@deprecated("SimpleValidator is deprecated. Use HtmlValidator.apply methods instead", "2.0.0")
object SimpleValidator {

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A])(matcher: A => Boolean): SimpleValidator[E, A, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher)

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A], result: R)(matcher: A => Boolean): SimpleValidator[E, A, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher, Some(result))
}
