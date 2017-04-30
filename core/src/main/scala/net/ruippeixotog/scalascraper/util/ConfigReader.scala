package net.ruippeixotog.scalascraper.util

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config

trait ConfigReader[A] {
  def read(conf: Config, path: String): A
}

object ConfigReader {
  def apply[A](readFunc: (Config, String) => A) = new ConfigReader[A] {
    def read(conf: Config, path: String) = readFunc(conf, path)
  }
}

trait ConfigReaders {
  implicit def boolConfReader: ConfigReader[Boolean] = ConfigReader(_.getBoolean(_))
  implicit def stringConfReader: ConfigReader[String] = ConfigReader(_.getString(_))
  implicit def intConfReader: ConfigReader[Int] = ConfigReader(_.getInt(_))
  implicit def doubleConfReader: ConfigReader[Double] = ConfigReader(_.getDouble(_))
  implicit def localDateConfReader: ConfigReader[LocalDate] = ConfigReader(_.getString(_).toLocalDate)
  implicit def dateTimeConfReader: ConfigReader[DateTime] = ConfigReader(_.getString(_).toDateTime)
}

object ConfigReaders extends ConfigReaders
