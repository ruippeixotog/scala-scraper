package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper.SimpleExtractor
import net.ruippeixotog.scalascraper.util._
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

trait ImplicitConversions {
  implicit def cssQueryAsExtractor(cssQuery: String) = SimpleExtractor(cssQuery)

  implicit def contentExtractorAsExtractor[C](contentExtractor: Elements => C) = {
    cssQuery: String => SimpleExtractor(cssQuery, contentExtractor)
  }

  implicit def elementAsElements(elem: Element) = new Elements(elem)

  implicit def projectValidatedSuccess[R, A](either: Validated[R, A]) = either.right
}

object ImplicitConversions extends ImplicitConversions
