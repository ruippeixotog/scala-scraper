package net.ruippeixotog.scalascraper

import java.io.Closeable

package object util {
  @deprecated("Validated was replaced by Either as the result type of validations", "2.0.0")
  type Validated[+R, +A] = Either[R, A]

  def using[A <: Closeable, R](closeable: A)(f: A => R): R =
    try f(closeable) finally closeable.close()
}
