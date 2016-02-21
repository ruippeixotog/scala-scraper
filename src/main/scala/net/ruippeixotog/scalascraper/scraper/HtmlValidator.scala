package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import net.ruippeixotog.scalascraper.model.{ ElementQuery, Element }
import net.ruippeixotog.scalascraper.util.ConfigReader

import scala.util.Try

trait HtmlValidator[+R] {
  def matches(doc: ElementQuery): Boolean
  def result: Option[R]
}

object HtmlValidator {

  def fromConfig[R](conf: Config)(implicit reader: ConfigReader[R]): HtmlValidator[R] = {
    if (conf.hasPath("exists")) {
      val extractor = SimpleExtractor[ElementQuery, ElementQuery](
        conf.getString("select.query"),
        ContentExtractors.elements, ContentParsers.asIs)

      val matcher = conf.getBoolean("exists") == (_: Iterable[Element]).nonEmpty
      val result = Try(reader.read(conf, "status")).toOption
      SimpleValidator(extractor, matcher, result)

    } else {
      val extractor = HtmlExtractor.fromConfig[String](conf.getConfig("select"))
      val matchIfTrue = if (conf.hasPath("invert")) !conf.getBoolean("invert") else true
      val matcher = conf.getString("match").r.pattern.matcher(_: String).matches() == matchIfTrue
      val result = Try(reader.read(conf, "status")).toOption
      SimpleValidator(extractor, matcher, result)
    }
  }

  val matchAll = new HtmlValidator[Nothing] {
    def matches(doc: ElementQuery) = true
    def result = None
  }

  val matchNothing = new HtmlValidator[Nothing] {
    def matches(doc: ElementQuery) = false
    def result = None
  }

  def matchAll[R](res: R) = new HtmlValidator[R] {
    def matches(doc: ElementQuery) = true
    def result = Some(res)
  }

  def matchNothing[R](res: R) = new HtmlValidator[R] {
    def matches(doc: ElementQuery) = false
    def result = Some(res)
  }
}

case class SimpleValidator[A, +R](
    htmlExtractor: HtmlExtractor[A],
    matcher: A => Boolean,
    result: Option[R] = None) extends HtmlValidator[R] {

  def matches(doc: ElementQuery) = Try(htmlExtractor.extract(doc)).map(matcher).getOrElse(false)

  def withResult[R2](res: R2) = SimpleValidator(htmlExtractor, matcher, Some(res))
  def withoutResult = SimpleValidator(htmlExtractor, matcher, None)
}

object SimpleValidator {

  def apply[A, R](htmlExtractor: HtmlExtractor[A])(matcher: A => Boolean): SimpleValidator[A, R] =
    SimpleValidator[A, R](htmlExtractor, matcher)

  def apply[A, R](htmlExtractor: HtmlExtractor[A], result: R)(matcher: A => Boolean): SimpleValidator[A, R] =
    SimpleValidator[A, R](htmlExtractor, matcher, Some(result))
}
