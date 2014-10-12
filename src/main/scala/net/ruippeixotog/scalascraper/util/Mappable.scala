package net.ruippeixotog.scalascraper.util

import scala.concurrent.{ExecutionContext, Future}

trait Mappable[F[_], A] {
  def mmap[B](f: A => B): F[B]
}

trait MappableInstances {
  type Id[A] = A

  implicit def anyAsMappable[A](a: A) = new Mappable[Id, A] {
    def mmap[B](f: A => B): B = f(a)
  }

  implicit def validatedAsMappable[A, R](v: Validated[A, R]) =
    new Mappable[({ type F[X] = Validated[X, R] })#F, A] {
      def mmap[B](f: A => B): Validated[B, R] = v.left.map(f)
    }

  implicit def optionAsMappable[A](opt: Option[A]) = new Mappable[Option, A] {
    def mmap[B](f: A => B) = opt.map(f)
  }

  implicit def futureAsMappable[A](fut: Future[A])(implicit ec: ExecutionContext) =
    new Mappable[Future, A] {
      def mmap[B](f: A => B) = fut.map(f)
    }
}

object MappableInstances extends MappableInstances
