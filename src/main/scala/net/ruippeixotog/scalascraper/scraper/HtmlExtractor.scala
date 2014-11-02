package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import org.joda.time.DateTime
import org.joda.time.format._
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.collection.convert.WrapAsScala._
import scala.util.matching.Regex
import scalaz.Monad

trait HtmlExtractor[+A] {
  def extract(doc: Elements): A
}

trait HtmlExtractorInstances {

  implicit val extractorInstance = new Monad[HtmlExtractor] {
    def point[A](a: => A) = new HtmlExtractor[A] {
      def extract(doc: Elements) = a
    }

    def bind[A, B](fa: HtmlExtractor[A])(f: A => HtmlExtractor[B]) = new HtmlExtractor[B] {
      def extract(doc: Elements) = f(fa.extract(doc)).extract(doc)
    }

    override def map[A, B](fa: HtmlExtractor[A])(f: A => B) = new HtmlExtractor[B] {
      def extract(doc: Elements) = f(fa.extract(doc))
    }
  }
}

object HtmlExtractor extends HtmlExtractorInstances {

  def fromConfig[A](conf: Config) = {
    import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
    import net.ruippeixotog.scalascraper.scraper.ContentParsers._

    val cssQuery = conf.getString("query")

    val contentExtractor =
      if(conf.hasPath("attr")) attr(conf.getString("attr")) else allText

    val contentParser =
      if(conf.hasPath("date-format"))
        asDate(conf.getString("date-format"))
      else if(conf.hasPath("date-formats"))
        asDate(conf.getStringList("date-formats"): _*)
      else if(conf.hasPath("regex-format"))
        regexMatch(conf.getString("regex-format"))
      else
        asIs[String]

    SimpleExtractor(cssQuery, contentExtractor, contentParser.andThen(_.asInstanceOf[A]))
  }
}

case class SimpleExtractor[C, +A](cssQuery: String,
                                  contentExtractor: Elements => C,
                                  contentParser: C => A) extends HtmlExtractor[A] {

  def extract(doc: Elements) = contentParser(contentExtractor(doc.select(cssQuery)))

  def withQuery(cssQuery: String) = copy(cssQuery = cssQuery)

  def extractWith[C2](contentExtractor: Elements => C) = copy(contentExtractor = contentExtractor)

  def extractWith[C2, A2](contentExtractor: Elements => C2, contentParser: C2 => A2) =
    copy(contentExtractor = contentExtractor, contentParser = contentParser)

  def parseWith[A2](contentExtractor: C => A2) = copy(contentParser = contentParser)
}

object SimpleExtractor {

  def apply(cssQuery: String): SimpleExtractor[Seq[String], Seq[String]] =
    SimpleExtractor(cssQuery, ContentExtractors.texts, ContentParsers.asIs)

  def apply[C](cssQuery: String, contentExtractor: Elements => C): SimpleExtractor[C, C] =
    SimpleExtractor(cssQuery, contentExtractor, ContentParsers.asIs)
}

object ContentExtractors {
  def element: Elements => Element = _.head
  def elements: Elements => Elements = identity
  def elementList: Elements => List[Element] = _.toList

  def text: Elements => String = _.head.text
  def texts: Elements => Seq[String] = _.map(_.text)
  def allText: Elements => String = _.text

  def attr(attr: String): Elements => String = _.attr(attr)
  def attrs(attr: String): Elements => Seq[String] = _.map(_.attr(attr))

  def formData: Elements => Map[String, String] =
    _.select("input").map { e => e.attr("name") -> e.attr("value") }.toMap

  def formDataAndAction: Elements => (Map[String, String], String) = { elems =>
    (formData(elems), elems.attr("action"))
  }
}

object ContentParsers {
  def asIs[C] = identity[C] _

  def asInt: String => Int = _.toInt
  def asDouble: String => Double = _.toDouble

  def asDate(dateFormats: String*): String => DateTime = {
    val formatter = new DateTimeFormatterBuilder().append(null,
      dateFormats.map(DateTimeFormat.forPattern(_).getParser).toArray).toFormatter
    formatter.parseDateTime
  }

  class RegexMatch private[ContentParsers](regex: Regex) extends (String => String) {
    def apply(content: String) = regex.findFirstIn(content).get
    def captured: String => String = regex.findFirstMatchIn(_).get.subgroups.head
    def allCaptured: String => List[String] = regex.findFirstMatchIn(_).get.subgroups
  }

  class RegexMatches private[ContentParsers](regex: Regex) extends (String => Iterator[String]) {
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
