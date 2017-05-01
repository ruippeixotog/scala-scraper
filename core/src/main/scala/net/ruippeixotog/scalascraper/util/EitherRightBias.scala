package net.ruippeixotog.scalascraper.util

/**
  * Provides a compatibility implicit conversion to make `Either` right-biased and therefore able to be used in
  * for-comprehensions. Useful for Scala 2.11 users who cannot upgrade to Scala 2.12 for some reason.
  */
object EitherRightBias {
  implicit def eitherRightBias[R, A](either: Either[R, A]) = either.right
}
