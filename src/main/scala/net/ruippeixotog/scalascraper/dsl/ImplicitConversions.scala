package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.scraper.{ HtmlExtractor, SimpleExtractor }
import net.ruippeixotog.scalascraper.util._

trait ImplicitConversions {

  implicit def cssQueryAsExtractor(cssQuery: String): HtmlExtractor[Iterable[String]] =
    SimpleExtractor(cssQuery)

  implicit def contentExtractorAsExtractor[C](contentExtractor: ElementQuery => C): String => HtmlExtractor[C] = {
    cssQuery: String => SimpleExtractor(cssQuery, contentExtractor)
  }

  implicit def elementAsElementQuery(elem: Element) = ElementQuery(elem)
  implicit def documentAsElementQuery(doc: Document) = ElementQuery(doc.root)

  implicit def projectValidatedSuccess[R, A](either: Validated[R, A]) = either.right
}

object ImplicitConversions extends ImplicitConversions
