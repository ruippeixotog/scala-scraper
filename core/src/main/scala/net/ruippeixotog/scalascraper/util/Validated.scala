package net.ruippeixotog.scalascraper.util

@deprecated("Validated was replaced by Either as the result type of validations", "2.0.0")
object Validated {
  @inline final def success[R, A](succ: A) = VSuccess[R, A](succ)
  @inline final def failure[R, A](succ: R) = VFailure[R, A](succ)

  object VSuccess {
    def apply[R, A](a: A): Validated[R, A] = Right(a)
    def unapply[R, A](either: Validated[R, A]) = either.right.toOption
  }

  object VFailure {
    def apply[R, A](res: R): Validated[R, A] = Left(res)
    def unapply[R, A](either: Validated[R, A]) = either.left.toOption
  }
}
