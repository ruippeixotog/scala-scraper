package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import net.ruippeixotog.scalascraper.model.{ ElementQuery, Element }
import net.ruippeixotog.scalascraper.util.ConfigReader

import scala.util.Try

trait HtmlValidator[-E <: Element, +R] {
  def matches(doc: ElementQuery[E]): Boolean
  def result: Option[R]
}

object HtmlValidator {

  def fromConfig[R](conf: Config)(implicit reader: ConfigReader[R]): HtmlValidator[Element, R] = {
    if (conf.hasPath("exists")) {
      val extractor = SimpleExtractor(
        conf.getString("select.query"),
        ContentExtractors.elements, ContentParsers.asIs[ElementQuery[Element]])

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

  val matchAll = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = true
    def result = None
  }

  val matchNothing = new HtmlValidator[Element, Nothing] {
    def matches(doc: ElementQuery[Element]) = false
    def result = None
  }

  def matchAll[R](res: R) = new HtmlValidator[Element, R] {
    def matches(doc: ElementQuery[Element]) = true
    def result = Some(res)
  }

  def matchNothing[R](res: R) = new HtmlValidator[Element, R] {
    def matches(doc: ElementQuery[Element]) = false
    def result = Some(res)
  }
}

case class SimpleValidator[-E <: Element, A, +R](
    htmlExtractor: HtmlExtractor[E, A],
    matcher: A => Boolean,
    result: Option[R] = None) extends HtmlValidator[E, R] {

  def matches(doc: ElementQuery[E]) = Try(htmlExtractor.extract(doc)).map(matcher).getOrElse(false)

  def withResult[R2](res: R2) = SimpleValidator(htmlExtractor, matcher, Some(res))
  def withoutResult = SimpleValidator(htmlExtractor, matcher, None)
}

object SimpleValidator {

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A])(matcher: A => Boolean): SimpleValidator[E, A, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher)

  def apply[E <: Element, A, R](htmlExtractor: HtmlExtractor[E, A], result: R)(matcher: A => Boolean): SimpleValidator[E, A, R] =
    SimpleValidator[E, A, R](htmlExtractor, matcher, Some(result))
}
