package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper._

object DSL extends ImplicitConversions with ScrapingOps with ConfigLoadingHelpers {
  val extractor = SimpleExtractor

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
