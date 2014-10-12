package net.ruippeixotog.scalascraper.dsl

import com.typesafe.config.{Config, ConfigFactory}
import net.ruippeixotog.scalascraper.scraper._
import net.ruippeixotog.scalascraper.util._

import scala.collection.convert.WrapAsScala._

trait ConfigLoadingHelpers extends ConfigReaders {

  def matcherAt[R: ConfigReader](config: Config): HtmlStatusMatcher[R] =
    HtmlStatusMatcher.fromConfig[R](config)

  @inline final def matcherAt[R: ConfigReader](config: Config, path: String): HtmlStatusMatcher[R] =
    matcherAt[R](config.getConfig(path))

  @inline final def matcherAt[R: ConfigReader](path: String): HtmlStatusMatcher[R] =
    matcherAt[R](ConfigFactory.load.getConfig(path))

  def matchersAt[R: ConfigReader](configs: Seq[Config]): Seq[HtmlStatusMatcher[R]] =
    configs.map(matcherAt[R])

  @inline final def matchersAt[R: ConfigReader](config: Config, path: String): Seq[HtmlStatusMatcher[R]] =
    matchersAt[R](config.getConfigList(path))

  @inline final def matchersAt[R: ConfigReader](path: String): Seq[HtmlStatusMatcher[R]] =
    matchersAt[R](ConfigFactory.load.getConfigList(path))

  def extractorAt[A](config: Config): SimpleExtractor[String, A] =
    HtmlExtractor.fromConfig[A](config)

  @inline final def extractorAt[A](config: Config, path: String): SimpleExtractor[String, A] =
    extractorAt[A](config.getConfig(path))

  @inline final def extractorAt[A](path: String): SimpleExtractor[String, A] =
    extractorAt[A](ConfigFactory.load.getConfig(path))
}

object ConfigLoadingHelpers extends ConfigLoadingHelpers
