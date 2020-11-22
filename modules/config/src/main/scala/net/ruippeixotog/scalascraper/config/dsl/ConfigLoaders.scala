package net.ruippeixotog.scalascraper.config.dsl

import scala.collection.JavaConverters._

import com.typesafe.config.{Config, ConfigFactory}

import net.ruippeixotog.scalascraper.config.util.ConfigReader
import net.ruippeixotog.scalascraper.config.{ConfigHtmlExtractor, ConfigHtmlValidator}
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper._

trait ConfigLoaders {

  implicit def errorReader: ConfigReader[Nothing] =
    ConfigReader { (_, _) =>
      throw new Exception("A type must be provided for reading the result of a validator from config")
    }

  def validatorAt[R: ConfigReader](config: Config): HtmlValidator[Element, R] =
    ConfigHtmlValidator[R](config)

  @inline final def validatorAt[R: ConfigReader](config: Config, path: String): HtmlValidator[Element, R] =
    validatorAt[R](config.getConfig(path))

  @inline final def validatorAt[R: ConfigReader](path: String): HtmlValidator[Element, R] =
    validatorAt[R](ConfigFactory.load.getConfig(path))

  def validatorsAt[R: ConfigReader](configs: Seq[Config]): Seq[HtmlValidator[Element, R]] =
    configs.map(validatorAt[R])

  @inline final def validatorsAt[R: ConfigReader](config: Config, path: String): Seq[HtmlValidator[Element, R]] =
    validatorsAt[R](config.getConfigList(path).asScala.toSeq)

  @inline final def validatorsAt[R: ConfigReader](path: String): Seq[HtmlValidator[Element, R]] =
    validatorsAt[R](ConfigFactory.load.getConfigList(path).asScala.toSeq)

  def extractorAt[A](config: Config): HtmlExtractor[Element, A] =
    ConfigHtmlExtractor[A](config)

  @inline final def extractorAt[A](config: Config, path: String): HtmlExtractor[Element, A] =
    extractorAt[A](config.getConfig(path))

  @inline final def extractorAt[A](path: String): HtmlExtractor[Element, A] =
    extractorAt[A](ConfigFactory.load.getConfig(path))
}

object ConfigLoaders extends ConfigLoaders
