package net.ruippeixotog.scalascraper.scraper

import scala.util.matching.Regex

import org.joda.time.format._
import org.joda.time.{ DateTime, DateTimeZone }

object ContentParsers {
  def asIs[C] = identity[C] _

  val asInt: String => Int = _.toInt
  val asDouble: String => Double = _.toDouble

  @deprecated("Use either asDateTime or asLocalDate", "2.0.0")
  def asDate(formats: String*) = new AsDateTime(formats)

  def asDateTime(formats: String*) = new AsDateTime(formats)
  def asLocalDate(formats: String*) = new AsLocalDate(formats)

  def regexMatch(regex: String): RegexMatch = new RegexMatch(regex.r)
  def regexMatch(regex: Regex): RegexMatch = new RegexMatch(regex)
  def regexMatches(regex: String): RegexMatches = new RegexMatches(regex.r)
  def regexMatches(regex: Regex): RegexMatches = new RegexMatches(regex)

  def seq[C, A](parser: C => A): TraversableOnce[C] => TraversableOnce[A] = _.map(parser)

  class AsJodaTime[A](formats: Seq[String], parse: (DateTimeFormatter, String) => A) extends (String => A) {
    protected[this] lazy val dateParsers = formats.map(DateTimeFormat.forPattern(_).getParser)
    protected[this] lazy val formatter = new DateTimeFormatterBuilder().append(null, dateParsers.toArray).toFormatter

    def apply(content: String) = parse(formatter, content)
  }

  class AsLocalDate private[ContentParsers] (formats: Seq[String]) extends AsJodaTime(formats, _.parseLocalDate(_))

  class AsDateTime private[ContentParsers] (formats: Seq[String]) extends AsJodaTime(formats, _.parseDateTime(_)) {
    def withZone(tz: DateTimeZone): String => DateTime = formatter.withZone(tz).parseDateTime(_).withZone(tz)
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
}
