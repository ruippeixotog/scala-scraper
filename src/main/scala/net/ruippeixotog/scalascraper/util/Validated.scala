package net.ruippeixotog.scalascraper.util

object Validated {
  @inline final def success[A, R](succ: A) = VSuccess[A, R](succ)
  @inline final def failure[A, R](succ: R) = VFailure[A, R](succ)

  object VSuccess {
    def apply[A, B](a: A): Validated[A, B] = Left(a)
    def unapply[A, R](either: Validated[A, R]) = either.left.toOption
  }

  object VFailure {
    def apply[A, R](res: R): Validated[A, R] = Right(res)
    def unapply[A, R](either: Validated[A, R]) = either.right.toOption
  }
}
