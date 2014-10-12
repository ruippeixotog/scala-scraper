package net.ruippeixotog.scalascraper

import com.typesafe.config.Config

package object util {
  type ConfigReader[A] = (Config, String) => A
  type Validated[A, R] = Either[A, R]
}
