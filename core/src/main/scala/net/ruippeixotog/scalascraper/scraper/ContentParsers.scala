package net.ruippeixotog.scalascraper.scraper

import scala.util.matching.Regex

import org.joda.time.format._
import org.joda.time.{ DateTime, DateTimeZone }

/**
  * An object containing functions for parsing extracted content. They can be used together with the DSL `extractor`
  * method or by calling `map` on a `HtmlExtractor` with them.
  */
object ContentParsers {

  /**
    * Leaves the extracted content as is.
    */
  def asIs[C] = identity[C] _

  /**
    * Parses text content as an `Int` value.
    */
  val asInt: String => Int = _.toInt

  /**
    * Parses text content as a `Double` value.
    */
  val asDouble: String => Double = _.toDouble

  @deprecated("Use either asDateTime or asLocalDate", "2.0.0")
  def asDate(formats: String*) = new AsDateTime(formats)

  /**
    * Parses text content as a `DateTime` using one of a list of formats.
    *
    * @param formats the list of possible date formats
    * @return a content parser for parsing text content as a `DateTime`.
    */
  def asDateTime(formats: String*) = new AsDateTime(formats)

  /**
    * Parses text content as a `LocalDate` using one of a list of formats.
    *
    * @param formats the list of possible date formats
    * @return a content parser for parsing text content as a `DateTime`.
    */
  def asLocalDate(formats: String*) = new AsLocalDate(formats)

  /**
    * Matches text content against a regex and returns the first match.
    *
    * @param regex the regex to match the text content against
    * @return a content parser for matching text content against a regex and returning the first match.
    */
  def regexMatch(regex: String): RegexMatch = new RegexMatch(regex.r)

  /**
    * Matches text content against a regex and returns the first match.
    *
    * @param regex the regex to match the text content against
    * @return a content parser for matching text content against a regex and returning the first match.
    */
  def regexMatch(regex: Regex): RegexMatch = new RegexMatch(regex)

  /**
    * Matches text content against a regex and returns all the matches.
    *
    * @param regex the regex to match the text content against
    * @return a content parser for matching text content against a regex and returning all the matches.
    */
  def regexMatches(regex: String): RegexMatches = new RegexMatches(regex.r)

  /**
    * Matches text content against a regex and returns all the matches.
    *
    * @param regex the regex to match the text content against
    * @return a content parser for matching text content against a regex and returning all the matches.
    */
  def regexMatches(regex: Regex): RegexMatches = new RegexMatches(regex)

  /**
    * Lifts a content parser to work on sequences of elements.
    *
    * @param parser the content parser to lift
    * @return a content parser for parsing a sequence of elements by applying `parser` to each of them.
    */
  def seq[C, A](parser: C => A): TraversableOnce[C] => TraversableOnce[A] = _.map(parser)

  /**
    * A content parser with extra options for parsing joda-time models.
    */
  class AsJodaTime[A](formats: Seq[String], parse: (DateTimeFormatter, String) => A) extends (String => A) {
    protected[this] lazy val dateParsers = formats.map(DateTimeFormat.forPattern(_).getParser)
    protected[this] lazy val formatter = new DateTimeFormatterBuilder().append(null, dateParsers.toArray).toFormatter

    def apply(content: String) = parse(formatter, content)
  }

  /**
    * A content parser with extra options for parsing `LocalDate`s.
    */
  class AsLocalDate private[ContentParsers] (formats: Seq[String]) extends AsJodaTime(formats, _.parseLocalDate(_))

  /**
    * A content parser with extra options for parsing `DateTime`s.
    */
  class AsDateTime private[ContentParsers] (formats: Seq[String]) extends AsJodaTime(formats, _.parseDateTime(_)) {

    /**
      * Parses text content as a `DateTime` using a provided default time zone.
      *
      * @param tz the default timezone to use if there is none specified in the format
      * @return a content parser for parsing text content as a `DateTime` using `tz` as default time zone.
      */
    def withZone(tz: DateTimeZone): String => DateTime = formatter.withZone(tz).parseDateTime(_).withZone(tz)
  }

  /**
    * A content parser with extra options for the retrieval of the first match of a regex.
    */
  class RegexMatch private[ContentParsers] (regex: Regex) extends (String => String) {
    def apply(content: String) = regex.findFirstIn(content).get

    /**
      * Matches text content against a regex and returns the first captured group of the first match.
      */
    def captured: String => String = regex.findFirstMatchIn(_).get.subgroups.head

    /**
      * Matches text content against a regex and returns all the captured groups of the first match.
      */
    def allCaptured: String => List[String] = regex.findFirstMatchIn(_).get.subgroups
  }

  /**
    * A content parser with extra options for the retrieval of all the matches of a regex.
    */
  class RegexMatches private[ContentParsers] (regex: Regex) extends (String => Iterator[String]) {
    def apply(content: String) = regex.findAllIn(content)

    /**
      * Matches text content against a regex and returns the first captured group of all the matches.
      */
    def captured: String => Iterator[String] = regex.findAllMatchIn(_).map(_.subgroups.head)

    /**
      * Matches text content against a regex and returns all the captured groups of all the matches.
      */
    def allCaptured: String => Iterator[List[String]] = regex.findAllMatchIn(_).map(_.subgroups)
  }
}
