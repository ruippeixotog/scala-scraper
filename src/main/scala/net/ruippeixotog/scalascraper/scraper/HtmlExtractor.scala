package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import net.ruippeixotog.scalascraper.model.{ ElementQuery, Element }
import org.joda.time.DateTime
import org.joda.time.format._

import scala.collection.convert.WrapAsScala._
import scala.util.matching.Regex
import scalaz.Monad

import ContentExtractors._
import ContentParsers._

trait HtmlExtractor[+A] {
  def extract(doc: ElementQuery): A
}

trait HtmlExtractorInstances {

  implicit val extractorInstance = new Monad[HtmlExtractor] {
    def point[A](a: => A) = new HtmlExtractor[A] {
      def extract(doc: ElementQuery) = a
    }

    def bind[A, B](fa: HtmlExtractor[A])(f: A => HtmlExtractor[B]) = new HtmlExtractor[B] {
      def extract(doc: ElementQuery) = f(fa.extract(doc)).extract(doc)
    }

    override def map[A, B](fa: HtmlExtractor[A])(f: A => B) = new HtmlExtractor[B] {
      def extract(doc: ElementQuery) = f(fa.extract(doc))
    }
  }
}

object HtmlExtractor extends HtmlExtractorInstances {

  def fromConfig[A](conf: Config) = {
    val cssQuery = conf.getString("query")

    val contentExtractor =
      if (conf.hasPath("attr")) attr(conf.getString("attr")) else allText

    val contentParser =
      if (conf.hasPath("date-format"))
        asDate(conf.getString("date-format"))
      else if (conf.hasPath("date-formats"))
        asDate(conf.getStringList("date-formats"): _*)
      else if (conf.hasPath("regex-format"))
        regexMatch(conf.getString("regex-format"))
      else
        asIs[String]

    SimpleExtractor(cssQuery, contentExtractor, contentParser.andThen(_.asInstanceOf[A]))
  }
}

case class SimpleExtractor[C, +A](
    cssQuery: String,
    contentExtractor: ElementQuery => C,
    contentParser: C => A) extends HtmlExtractor[A] {

  def extract(doc: ElementQuery) = contentParser(contentExtractor(doc.select(cssQuery)))

  def withQuery(cssQuery: String) = copy(cssQuery = cssQuery)

  def extractWith[C2](contentExtractor: ElementQuery => C) = copy(contentExtractor = contentExtractor)

  def extractWith[C2, A2](contentExtractor: ElementQuery => C2, contentParser: C2 => A2) =
    copy(contentExtractor = contentExtractor, contentParser = contentParser)

  def parseWith[A2](contentExtractor: C => A2) = copy(contentParser = contentParser)
}

object SimpleExtractor {

  def apply(cssQuery: String): SimpleExtractor[Iterable[String], Iterable[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs)

  def apply[C](cssQuery: String, contentExtractor: ElementQuery => C): SimpleExtractor[C, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs)
}

object ContentExtractors {
  val element: ElementQuery => Element = _.head
  val elements: ElementQuery => ElementQuery = identity
  val elementList: ElementQuery => List[Element] = _.toList

  val text: ElementQuery => String = _.head.text
  val texts: ElementQuery => Iterable[String] = _.map(_.text)
  val allText: ElementQuery => String = _.map(_.text).mkString

  def attr(attr: String): ElementQuery => String = _.head.attr(attr)
  def attrs(attr: String): ElementQuery => Iterable[String] = _.map(_.attr(attr))

  def formData: ElementQuery => Map[String, String] =
    _.map(_.select("input").map { e => e.attr("name") -> e.attr("value") }.toMap).reduce(_ ++ _)

  def formDataAndAction: ElementQuery => (Map[String, String], String) = { elems =>
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
