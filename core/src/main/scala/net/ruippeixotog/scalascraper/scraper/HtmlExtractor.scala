package net.ruippeixotog.scalascraper.scraper

import scala.util.matching.Regex

import org.joda.time.DateTime
import org.joda.time.format._
import scalaz.Monad

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

trait HtmlExtractor[-E <: Element, +A] {
  def extract(doc: ElementQuery[E]): A
}

trait HtmlExtractorInstances {

  implicit def extractorInstance[E <: Element] = new Monad[({ type t[A] = HtmlExtractor[E, A] })#t] {
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

object HtmlExtractor extends HtmlExtractorInstances

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

object SimpleExtractor {

  def apply(cssQuery: String): SimpleExtractor[Element, Iterable[String], Iterable[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs)

  def apply[E <: Element, C](cssQuery: String, contentExtractor: ElementQuery[E] => C): SimpleExtractor[E, C, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs)
}
