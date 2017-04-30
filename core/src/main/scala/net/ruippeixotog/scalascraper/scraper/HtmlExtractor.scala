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

object ContentExtractors {

  val element: ElementQuery[Element] => Element = _.head
  val elements: ElementQuery[Element] => ElementQuery[Element] = identity
  val elementList: ElementQuery[Element] => List[Element] = _.toList

  def elementOf[E <: Element.Upper[E]]: ElementQuery[E] => E = _.head
  def elementsOf[E <: Element.Upper[E]]: ElementQuery[E] => ElementQuery[E] = identity
  def elementListOf[E <: Element.Upper[E]]: ElementQuery[E] => List[E] = _.toList

  val text: ElementQuery[Element] => String = _.head.text
  val texts: ElementQuery[Element] => Iterable[String] = _.map(_.text)
  val allText: ElementQuery[Element] => String = _.map(_.text).mkString

  def attr(attr: String): ElementQuery[Element] => String = _.head.attr(attr)
  def attrs(attr: String): ElementQuery[Element] => Iterable[String] = _.map(_.attr(attr))

  // TODO add support for <select> and <textarea> elements
  // TODO add proper support for checkboxes and radio buttons
  // See: https://www.w3.org/TR/html5/forms.html#constructing-form-data-set
  val formData: ElementQuery[Element] => Map[String, String] =
    _.select("input")
      .filter(_.hasAttr("name"))
      .map { e => e.attr("name") -> (if (e.hasAttr("value")) e.attr("value") else "") }
      .toMap

  val formDataAndAction: ElementQuery[Element] => (Map[String, String], String) = { elems =>
    (formData(elems), attr("action")(elems))
  }
}

object ContentParsers {
  def asIs[C] = identity[C] _

  val asInt: String => Int = _.toInt
  val asDouble: String => Double = _.toDouble

  def asDate(dateFormats: String*): String => DateTime = {
    val dateParsers = dateFormats.map(DateTimeFormat.forPattern(_).getParser)
    val formatter = new DateTimeFormatterBuilder().append(null, dateParsers.toArray).toFormatter
    formatter.parseDateTime
  }

  class RegexMatch private[ContentParsers] (regex: Regex) extends (String => String) {
    def apply(content: String) = regex.findFirstIn(content).get
    def captured: String => String = regex.findFirstMatchIn(_).get.subgroups.head
    def allCaptured: String => List[String] = regex.findFirstMatchIn(_).get.subgroups
  }

  class RegexMatches private[ContentParsers] (regex: Regex) extends (String => Iterator[String]) {
    def apply(content: String) = regex.findAllIn(content)
    def captured: String => Iterator[String] = regex.findAllMatchIn(_).map(_.subgroups.head)
    def allCaptured: String => Iterator[List[String]] = regex.findAllMatchIn(_).map(_.subgroups)
  }

  def regexMatch(regex: String): RegexMatch = new RegexMatch(regex.r)
  def regexMatch(regex: Regex): RegexMatch = new RegexMatch(regex)
  def regexMatches(regex: String): RegexMatches = new RegexMatches(regex.r)
  def regexMatches(regex: Regex): RegexMatches = new RegexMatches(regex)

  def seq[C, A](parser: C => A): TraversableOnce[C] => TraversableOnce[A] = _.map(parser)
}
