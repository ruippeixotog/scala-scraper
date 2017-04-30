package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.{ Document, Element, ElementQuery }
import net.ruippeixotog.scalascraper.scraper.{ HtmlExtractor, HtmlValidator }
import net.ruippeixotog.scalascraper.util.Validated.{ VFailure, VSuccess }
import net.ruippeixotog.scalascraper.util._
import scala.util.Try
import scalaz._
import scalaz.syntax.ToFunctorOps

trait ScrapingOps extends syntax.ToIdOps with ToFunctorOps with std.AllInstances with IdInstances {

  class ElementsScrapingOps[+F[_]: Functor, A, E <: Element](val self: F[A])(implicit toQuery: ToQuery.Aux[A, E]) {
    @inline implicit private[this] def aToQuery(a: A) = toQuery(a)

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

    def >?>[B, C, D](extractor1: HtmlExtractor[E, B], extractor2: HtmlExtractor[E, C], extractor3: HtmlExtractor[E, D]) =
      self.map { doc =>
        val e1 = Try(extractor1.extract(doc)).toOption
        val e2 = Try(extractor2.extract(doc)).toOption
        val e3 = Try(extractor3.extract(doc)).toOption
        (e1, e2, e3)
      }

    def successIf[R](success: HtmlValidator[E, _]) = self.map { doc =>
      if (success.matches(doc)) VSuccess(doc) else VFailure(())
    }

    def errorIf[R](error: HtmlValidator[E, R]) = self.map { doc =>
      if (error.matches(doc)) VFailure[R, A](error.result.get) else VSuccess[R, A](doc)
    }

    def errorIf[R](errors: Seq[HtmlValidator[E, R]]) = self.map { doc =>
      errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
        if (res.isLeft || !error.matches(doc)) res else VFailure(error.result.get)
      }
    }

    def validateWith[R](
      success: HtmlValidator[E, _],
      errors: Seq[HtmlValidator[E, R]],
      default: => R = throw new ValidationException): F[Validated[R, A]] = {

      self.map { doc =>
        if (success.matches(doc)) VSuccess(doc)
        else errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
          if (res.isLeft || !error.matches(doc)) res else VFailure(error.result.get)
        }.fold(VFailure.apply, _ => VFailure(default))
      }
    }

    @inline final def ~/~[R](success: HtmlValidator[E, _]) = successIf(success)

    @inline final def ~/~[R](success: HtmlValidator[E, _], error: HtmlValidator[E, R]) =
      validateWith(success, error :: Nil)

    @inline final def ~/~[R](success: HtmlValidator[E, _], errors: Seq[HtmlValidator[E, R]]) =
      validateWith(success, errors)

    @inline final def ~/~[R](success: HtmlValidator[E, _], error: HtmlValidator[E, R], default: R) =
      validateWith(success, error :: Nil, default)

    @inline final def ~/~[R](success: HtmlValidator[E, _], errors: Seq[HtmlValidator[E, R]], default: R) =
      validateWith(success, errors, default)

    @inline final def and = self
  }

  implicit def deepFunctorOps[FA, A1, E <: Element](self: FA)(implicit df: DeepFunctor[FA] { type A = A1 }, conv: ToQuery.Aux[A1, E]) =
    new ElementsScrapingOps[df.F, df.A, E](df.asF(self))(df.f, conv)
}

object ScrapingOps extends ScrapingOps
