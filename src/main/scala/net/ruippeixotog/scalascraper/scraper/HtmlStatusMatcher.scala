package net.ruippeixotog.scalascraper.scraper

import com.typesafe.config.Config
import net.ruippeixotog.scalascraper.util.ConfigReader
import org.jsoup.select.Elements

import scala.collection.convert.WrapAsScala._

trait HtmlStatusMatcher[+R] {
  def matches(doc: Elements): Boolean
  def result: R
}

object HtmlStatusMatcher {

  def fromConfig[R](conf: Config)(implicit reader: ConfigReader[R]): HtmlStatusMatcher[R] = {
    if(conf.hasPath("exists")) {
      val extractor = SimpleExtractor[Elements, Elements](
        conf.getString("select.query"),
        ContentExtractors.elements, ContentParsers.asIs)

      val matcher = conf.getBoolean("exists") == (_: Elements).nonEmpty
      val result = reader(conf, "status")
      SimpleStatusMatcher(extractor, matcher, result)

    } else {
      val extractor = HtmlExtractor.fromConfig[String](conf.getConfig("select"))
      val matchIfTrue = if(conf.hasPath("invert")) !conf.getBoolean("invert") else true
      val matcher = conf.getString("match").r.pattern.matcher(_: String).matches() == matchIfTrue
      val result = reader(conf, "status")
      SimpleStatusMatcher(extractor, matcher, result)
    }
  }

  def matchAll[R](res: R) = new HtmlStatusMatcher[R] {
    def matches(doc: Elements) = true
    def result = res
  }

  def matchNothing[R](res: R) = new HtmlStatusMatcher[R] {
    def matches(doc: Elements) = false
    def result = res
  }
}

case class SimpleStatusMatcher[A, R](htmlExtractor: HtmlExtractor[A],
                                     matcher: A => Boolean,
                                     result: R) extends HtmlStatusMatcher[R] {

  def matches(doc: Elements) = matcher(htmlExtractor.extract(doc))
}
