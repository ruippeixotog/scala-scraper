package net.ruippeixotog.scalascraper.util

import scalaz._
import Scalaz._

sealed trait DeepFunctor[FA] {
  type A
  type F[_]
  def f: Functor[F]
  def asF(fa: FA): F[A]
}

trait LowerPriorityDeepFunctor {
  implicit def nil[A1] = new DeepFunctor[A1] {
    type A = A1
    type F[X] = X
    val f = implicitly[Functor[Id]]
    def asF(a: A) = a
  }
}

object DeepFunctor extends LowerPriorityDeepFunctor {
  implicit def cons[FRA, RA](implicit u: Unapply[Functor, FRA] { type A = RA }, rest: DeepFunctor[RA]) =
    new DeepFunctor[FRA] {
      type A = rest.A
      type F[X] = u.M[rest.F[X]]
      val f = u.TC.compose(rest.f)
      def asF(fa: FRA) = u.TC.map(u(fa)) { ma => rest.asF(ma) }
    }
}
