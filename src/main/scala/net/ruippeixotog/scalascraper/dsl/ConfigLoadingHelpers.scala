package net.ruippeixotog.scalascraper.dsl

import com.typesafe.config.{Config, ConfigFactory}
import net.ruippeixotog.scalascraper.scraper._
import net.ruippeixotog.scalascraper.util._

import scala.collection.convert.WrapAsScala._

trait ConfigLoadingHelpers extends ConfigReaders {

  implicit def errorReader: ConfigReader[Nothing] = (_, _) =>
    throw new Exception("A type must be provided for reading the result of a validator from config")

  def validatorAt[R: ConfigReader](config: Config): HtmlValidator[R] =
    HtmlValidator.fromConfig[R](config)

  @inline final def validatorAt[R: ConfigReader](config: Config, path: String): HtmlValidator[R] =
    validatorAt[R](config.getConfig(path))

  @inline final def validatorAt[R: ConfigReader](path: String): HtmlValidator[R] =
    validatorAt[R](ConfigFactory.load.getConfig(path))

  def validatorsAt[R: ConfigReader](configs: Seq[Config]): Seq[HtmlValidator[R]] =
    configs.map(validatorAt[R])

  @inline final def validatorsAt[R: ConfigReader](config: Config, path: String): Seq[HtmlValidator[R]] =
    validatorsAt[R](config.getConfigList(path))

  @inline final def validatorsAt[R: ConfigReader](path: String): Seq[HtmlValidator[R]] =
    validatorsAt[R](ConfigFactory.load.getConfigList(path))

  def extractorAt[A](config: Config): SimpleExtractor[String, A] =
    HtmlExtractor.fromConfig[A](config)

  @inline final def extractorAt[A](config: Config, path: String): SimpleExtractor[String, A] =
    extractorAt[A](config.getConfig(path))

  @inline final def extractorAt[A](path: String): SimpleExtractor[String, A] =
    extractorAt[A](ConfigFactory.load.getConfig(path))
}

object ConfigLoadingHelpers extends ConfigLoadingHelpers
