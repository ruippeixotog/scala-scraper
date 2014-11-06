package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper._

import scala.collection.convert.WrapAsScala

object DSL extends ImplicitConversions with ScrapingOps with ConfigLoadingHelpers with WrapAsScala {
  val extractor = SimpleExtractor
  val validator = SimpleValidator

  val Extract = ContentExtractors
  val Parse = ContentParsers
}
