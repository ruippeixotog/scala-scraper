package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import org.joda.time.DateTime
import org.joda.time.format._
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.collection.convert.WrapAsScala._
import scala.util.matching.Regex

trait HtmlExtractor[+A] {
  def extract(doc: Elements): A
}

object HtmlExtractor {

  def fromConfig[A](conf: Config) = {
    import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
    import net.ruippeixotog.scalascraper.scraper.ContentParsers._

    val cssQuery = conf.getString("query")

    val contentExtractor =
      if(conf.hasPath("attr")) attr(conf.getString("attr")) else allText

    val contentParser =
      if(conf.hasPath("date-format"))
        asDate(DateTimeFormat.forPattern(conf.getString("date-format")))
      else if(conf.hasPath("date-formats"))
        asDate(conf.getStringList("date-formats").map(DateTimeFormat.forPattern): _*)
      else if(conf.hasPath("regex-format"))
        withRegex(conf.getString("regex-format"))
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

  def formData: Elements => Map[String, String] =
    _.select("input").map { e => e.attr("name") -> e.attr("value") }.toMap

  def formDataAndAction: Elements => (Map[String, String], String) = { elems =>
    (formData(elems), elems.attr("action"))
  }
}

object ContentParsers {
  def asIs[C] = identity[C] _

  def asDate(dateFormats: DateTimeFormatter*): String => DateTime = {
    val formatter = new DateTimeFormatterBuilder().
        append(null, dateFormats.map(_.getParser).toArray).toFormatter

    formatter.parseDateTime
  }

  def withRegex(regex: String): String => String = regex.r.findFirstIn(_).get
  def withRegex(regex: Regex): String => String = regex.findFirstIn(_).get

  def seq[C, A](parser: C => A): TraversableOnce[C] => TraversableOnce[A] = _.map(parser)
}
