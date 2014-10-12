package net.ruippeixotog.scalascraper.util

import com.github.nscala_time.time.Imports._

trait ConfigReaders {
  implicit def boolConfReader: ConfigReader[Boolean] = _.getBoolean(_)
  implicit def stringConfReader: ConfigReader[String] = _.getString(_)
  implicit def intConfReader: ConfigReader[Int] = _.getInt(_)
  implicit def doubleConfReader: ConfigReader[Double] = _.getDouble(_)
  implicit def localDateConfReader: ConfigReader[LocalDate] = _.getString(_).toLocalDate
  implicit def dateTimeConfReader: ConfigReader[DateTime] = _.getString(_).toDateTime
}

object ConfigReaders extends ConfigReaders
