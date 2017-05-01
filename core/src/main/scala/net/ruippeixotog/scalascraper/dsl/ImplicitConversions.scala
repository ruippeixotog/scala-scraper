package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import net.ruippeixotog.scalascraper.util._

trait ImplicitConversions {

  implicit def cssQueryAsExtractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    HtmlExtractor(cssQuery)

  implicit def contentExtractorAsExtractor[E <: Element, C](contentExtractor: ElementQuery[E] => C): String => HtmlExtractor[E, C] = {
    cssQuery: String => HtmlExtractor(cssQuery, contentExtractor)
  }

  implicit def elementAsElementQuery[E <: Element.Upper[E]](elem: E) = ElementQuery(elem)
  implicit def documentAsElementQuery[D <: Document](doc: D) = ElementQuery(doc.root)
}

object ImplicitConversions extends ImplicitConversions
