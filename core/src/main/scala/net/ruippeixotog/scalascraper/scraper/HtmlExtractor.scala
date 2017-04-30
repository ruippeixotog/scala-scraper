package net.ruippeixotog.scalascraper.scraper

import scalaz.Monad

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

trait HtmlExtractor[-E <: Element, +A] {
  def extract(doc: ElementQuery[E]): A
}

object HtmlExtractor extends HtmlExtractorInstances {

  def apply(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs[Iterable[String]])

  def apply[E <: Element, C](cssQuery: String, contentExtractor: ElementQuery[E] => C): HtmlExtractor[E, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs[C])

  def apply[E <: Element, C, A](
    cssQuery: String, contentExtractor: ElementQuery[E] => C, contentParser: C => A): HtmlExtractor[E, A] = {

    SimpleExtractor(cssQuery, contentExtractor, contentParser)
  }
}

trait HtmlExtractorInstances {

  implicit def extractorMonad[E <: Element] = new Monad[({ type t[A] = HtmlExtractor[E, A] })#t] {
    def point[A](a: => A) = new HtmlExtractor[E, A] {
      def extract(doc: ElementQuery[E]) = a
    }

    def bind[A, B](fa: HtmlExtractor[E, A])(f: A => HtmlExtractor[E, B]) = new HtmlExtractor[E, B] {
      def extract(doc: ElementQuery[E]) = f(fa.extract(doc)).extract(doc)
    }

    override def map[A, B](fa: HtmlExtractor[E, A])(f: A => B) = new HtmlExtractor[E, B] {
      def extract(doc: ElementQuery[E]) = f(fa.extract(doc))
    }
  }
}

@deprecated("SimpleExtractor is deprecated. Use HtmlExtractor.apply methods instead", "2.0.0")
case class SimpleExtractor[-E <: Element, C, +A](
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

@deprecated("SimpleExtractor is deprecated. Use HtmlExtractor.apply methods instead", "2.0.0")
object SimpleExtractor {

  def apply(cssQuery: String): SimpleExtractor[Element, Iterable[String], Iterable[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs)

  def apply[E <: Element, C](cssQuery: String, contentExtractor: ElementQuery[E] => C): SimpleExtractor[E, C, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs)
}
