package net.ruippeixotog.scalascraper.scraper

import scalaz.Monad

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

trait HtmlExtractor[-E <: Element, +A] extends (ElementQuery[E] => A) {

  def extract(doc: ElementQuery[E]): A

  @inline final def apply(doc: ElementQuery[E]) = extract(doc)

  def map[B](f: A => B): HtmlExtractor[E, B] =
    HtmlExtractor { doc => f(extract(doc)) }

  def mapQuery(cssQuery: String): HtmlExtractor[E, A] =
    HtmlExtractor { doc => extract(doc.select(cssQuery)) }
}

object HtmlExtractor extends HtmlExtractorInstances {

  def apply[E <: Element, A](f: ElementQuery[E] => A): HtmlExtractor[E, A] = new HtmlExtractor[E, A] {
    def extract(doc: ElementQuery[E]): A = f(doc)
  }

  def forQuery(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    HtmlExtractor { doc => ContentExtractors.texts(doc.select(cssQuery)) }
}

trait HtmlExtractorInstances {

  implicit def extractorMonad[E <: Element] = new Monad[({ type t[A] = HtmlExtractor[E, A] })#t] {
    def point[A](a: => A) = new HtmlExtractor[E, A] {
      def extract(doc: ElementQuery[E]) = a
    }

    def bind[A, B](fa: HtmlExtractor[E, A])(f: A => HtmlExtractor[E, B]) = new HtmlExtractor[E, B] {
      def extract(doc: ElementQuery[E]) = f(fa.extract(doc)).extract(doc)
    }

    override def map[A, B](fa: HtmlExtractor[E, A])(f: A => B) = fa.map(f)
  }
}

@deprecated("Use HtmlExtractor constructor methods followed by map and mapQuery", "2.0.0")
case class SimpleExtractor[-E <: Element, C, +A] @deprecated(
  "Use `contentExtractor.mapQuery(cssQuery).map(contentParser)` instead", "2.0.0") (
    cssQuery: String,
    contentExtractor: ElementQuery[E] => C,
    contentParser: C => A) extends HtmlExtractor[E, A] {

  def extract(doc: ElementQuery[E]) = contentParser(contentExtractor(doc.select(cssQuery)))

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
