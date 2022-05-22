package net.ruippeixotog.scalascraper.util

import scalaz._
import Scalaz._

/** Type class that identifies a type `FA` as a nested sequence of type constructors applied to a type `A`, where each
  * one has a `Functor` instance. It destructures `FA` into a composite type constructor `F[_]` applied to type `A`,
  * along with the composite `Functor` for `F`.
  *
  * @tparam FA
  *   the type to destructure as mentioned above
  */
sealed trait DeepFunctor[FA] {
  type A
  type F[_]
  def f: Functor[F]
  def asF(fa: FA): F[A]
}

trait LowerPriorityDeepFunctor {
  implicit def nil[A1]: DeepFunctor.AuxA[A1, A1] { type F[X] = X } =
    new DeepFunctor[A1] {
      type A = A1
      type F[X] = X
      val f = implicitly[Functor[Id]]
      def asF(a: A) = a
    }
}

object DeepFunctor extends LowerPriorityDeepFunctor {
  type AuxA[FA, A0] = DeepFunctor[FA] { type A = A0 }

  implicit def cons[FRA, RA](implicit
      u: Unapply.AuxA[Functor, FRA, RA],
      rest: DeepFunctor[RA]
  ): DeepFunctor.AuxA[FRA, rest.A] { type F[X] = u.M[rest.F[X]] } =
    new DeepFunctor[FRA] {
      type A = rest.A
      type F[X] = u.M[rest.F[X]]
      val f = u.TC.compose(rest.f)
      def asF(fa: FRA) = u.TC.map(u(fa)) { ma => rest.asF(ma) }
    }
}
