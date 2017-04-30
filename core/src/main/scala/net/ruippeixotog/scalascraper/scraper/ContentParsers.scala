package net.ruippeixotog.scalascraper.scraper

import scala.util.matching.Regex

import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatterBuilder }

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
