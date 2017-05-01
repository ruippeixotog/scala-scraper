package net.ruippeixotog.scalascraper

package object util {
  @deprecated("Validated was replaced by Either as the result type of validations", "2.0.0")
  type Validated[+R, +A] = Either[R, A]
}
