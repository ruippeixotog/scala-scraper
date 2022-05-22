package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.{Element, ElementQuery}
import net.ruippeixotog.scalascraper.scraper.{HtmlExtractor, HtmlValidator}
import net.ruippeixotog.scalascraper.util._
import scala.util.Try

import scalaz._
import scalaz.syntax.FunctorSyntax

trait ScrapingOps extends syntax.ToIdOps with std.AllInstances with IdInstances {

  class ElementsScrapingOps[F[_]: Functor, A, E <: Element](val self: F[A])(implicit toQuery: ToQuery.Aux[A, E])
      extends FunctorSyntax[F] {

    override def F: Functor[F] = Functor[F]

    @inline implicit private[this] def aToQuery(a: A): ElementQuery[E] = toQuery(a)

    def extract[B](extractor: HtmlExtractor[E, B]) = self.map(extractor.extract(_))

    @inline final def apply[B](extractor: HtmlExtractor[E, B]) = extract(extractor)

    @inline final def >>[B](extractor: HtmlExtractor[E, B]) = extract(extractor)

    def >>[B, C](extractor1: HtmlExtractor[E, B], extractor2: HtmlExtractor[E, C]) =
      self.map { doc => (extractor1.extract(doc), extractor2.extract(doc)) }

    def >>[B, C, D](extractor1: HtmlExtractor[E, B], extractor2: HtmlExtractor[E, C], extractor3: HtmlExtractor[E, D]) =
      self.map { doc => (extractor1.extract(doc), extractor2.extract(doc), extractor3.extract(doc)) }

    def tryExtract[B](extractor: HtmlExtractor[E, B]) =
      self.map { doc => Try(extractor.extract(doc)).toOption }

    @inline final def tryApply[B](extractor: HtmlExtractor[E, B]) = tryExtract(extractor)

    @inline final def >?>[B](extractor: HtmlExtractor[E, B]) = tryExtract(extractor)

    def >?>[B, C](extractor1: HtmlExtractor[E, B], extractor2: HtmlExtractor[E, C]) =
      self.map { doc => (Try(extractor1.extract(doc)).toOption, Try(extractor2.extract(doc)).toOption) }

    def >?>[B, C, D](
        extractor1: HtmlExtractor[E, B],
        extractor2: HtmlExtractor[E, C],
        extractor3: HtmlExtractor[E, D]
    ) =
      self.map { doc =>
        val e1 = Try(extractor1.extract(doc)).toOption
        val e2 = Try(extractor2.extract(doc)).toOption
        val e3 = Try(extractor3.extract(doc)).toOption
        (e1, e2, e3)
      }

    def successIf[R](success: HtmlValidator[E, _]): F[Either[Unit, A]] =
      self.map { doc => if (success.matches(doc)) Right(doc) else Left(()) }

    def errorIf[R](error: HtmlValidator[E, R]): F[Either[R, A]] =
      self.map { doc => if (error.matches(doc)) Left(error.result.get) else Right(doc) }

    def errorIf[R](errors: Seq[HtmlValidator[E, R]]): F[Either[R, A]] = {
      self.map { doc =>
        errors.foldLeft[Either[R, A]](Right(doc)) { (res, error) =>
          if (res.isLeft || !error.matches(doc)) res else Left(error.result.get)
        }
      }
    }

    def validateWith[R](
        success: HtmlValidator[E, _],
        errors: Seq[HtmlValidator[E, R]],
        default: => R = throw new ValidationException
    ): F[Either[R, A]] = {

      self.map { doc =>
        if (success.matches(doc)) Right(doc)
        else
          errors
            .foldLeft[Either[R, A]](Right(doc)) { (res, error) =>
              if (res.isLeft || !error.matches(doc)) res else Left(error.result.get)
            }
            .fold(Left.apply, _ => Left(default))
      }
    }

    @inline final def >/~[R](success: HtmlValidator[E, _]) = successIf(success)

    @inline final def >/~[R](success: HtmlValidator[E, _], error: HtmlValidator[E, R]) =
      validateWith(success, error :: Nil)

    @inline final def >/~[R](success: HtmlValidator[E, _], errors: Seq[HtmlValidator[E, R]]) =
      validateWith(success, errors)

    @inline final def >/~[R](success: HtmlValidator[E, _], error: HtmlValidator[E, R], default: R) =
      validateWith(success, error :: Nil, default)

    @inline final def >/~[R](success: HtmlValidator[E, _], errors: Seq[HtmlValidator[E, R]], default: R) =
      validateWith(success, errors, default)
  }

  implicit def deepFunctorOps[FA, A, E <: Element](
      self: FA
  )(implicit df: DeepFunctor.AuxA[FA, A], conv: ToQuery.Aux[A, E]): ElementsScrapingOps[df.F, A, E] =
    new ElementsScrapingOps[df.F, A, E](df.asF(self))(df.f, conv)
}

object ScrapingOps extends ScrapingOps
