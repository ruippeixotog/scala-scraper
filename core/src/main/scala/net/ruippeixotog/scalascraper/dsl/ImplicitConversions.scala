package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.{ HtmlExtractor, SimpleExtractor }
import net.ruippeixotog.scalascraper.util._

trait ImplicitConversions {

  implicit def cssQueryAsExtractor(cssQuery: String): HtmlExtractor[Element, Iterable[String]] =
    SimpleExtractor(cssQuery)

  implicit def contentExtractorAsExtractor[E <: Element, C](contentExtractor: ElementQuery[E] => C): String => HtmlExtractor[E, C] = {
    cssQuery: String => SimpleExtractor(cssQuery, contentExtractor)
  }

  implicit def elementAsElementQuery[E <: Element.Upper[E]](elem: E) = ElementQuery(elem)
  implicit def documentAsElementQuery[D <: Document](doc: D) = ElementQuery(doc.root)

  implicit def projectValidatedSuccess[R, A](either: Validated[R, A]) = either.right
}

object ImplicitConversions extends ImplicitConversions
