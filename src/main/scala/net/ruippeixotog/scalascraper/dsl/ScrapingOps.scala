package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model.ElementQuery
import net.ruippeixotog.scalascraper.scraper.{ HtmlExtractor, HtmlValidator }
import net.ruippeixotog.scalascraper.util.Validated.{ VFailure, VSuccess }
import net.ruippeixotog.scalascraper.util._

import scala.util.Try
import scalaz._
import scalaz.syntax.ToFunctorOps

trait ScrapingOps extends syntax.ToIdOps with ToFunctorOps with std.AllInstances with IdInstances {

  implicit class ElementsScrapingOps[+F[_]: Functor, A <% ElementQuery](val self: F[A]) {

    def extract[B](extractor: HtmlExtractor[B]) = self.map(extractor.extract(_))

    @inline final def apply[B](extractor: HtmlExtractor[B]) = extract(extractor)

    @inline final def >>[B](extractor: HtmlExtractor[B]) = extract(extractor)

    def >>[B, C](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C]) =
      self.map { doc => (extractor1.extract(doc), extractor2.extract(doc)) }

    def >>[B, C, D](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C], extractor3: HtmlExtractor[D]) =
      self.map { doc => (extractor1.extract(doc), extractor2.extract(doc), extractor3.extract(doc)) }

    def tryExtract[B](extractor: HtmlExtractor[B]) =
      self.map { doc => Try(extractor.extract(doc)).toOption }

    @inline final def tryApply[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    @inline final def >?>[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    def >?>[B, C](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C]) =
      self.map { doc => (Try(extractor1.extract(doc)).toOption, Try(extractor2.extract(doc)).toOption) }

    def >?>[B, C, D](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C], extractor3: HtmlExtractor[C]) =
      self.map { doc =>
        val e1 = Try(extractor1.extract(doc)).toOption
        val e2 = Try(extractor2.extract(doc)).toOption
        val e3 = Try(extractor3.extract(doc)).toOption
        (e1, e2, e3)
      }

    def successIf[R](success: HtmlValidator[_]) = self.map { doc =>
      if (success.matches(doc)) VSuccess(doc) else VFailure(())
    }

    def errorIf[R](error: HtmlValidator[R]) = self.map { doc =>
      if (error.matches(doc)) VFailure[R, A](error.result.get) else VSuccess[R, A](doc)
    }

    def errorIf[R](errors: Seq[HtmlValidator[R]]) = self.map { doc =>
      errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
        if (res.isLeft || !error.matches(doc)) res else VFailure(error.result.get)
      }
    }

    def validateWith[R](
      success: HtmlValidator[_],
      errors: Seq[HtmlValidator[R]],
      default: => R = throw new ValidationException): F[Validated[R, A]] = {

      self.map { doc =>
        if (success.matches(doc)) VSuccess(doc)
        else errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
          if (res.isLeft || !error.matches(doc)) res else VFailure(error.result.get)
        }.fold(VFailure.apply, _ => VFailure(default))
      }
    }

    @inline final def ~/~[R](success: HtmlValidator[_]) = successIf(success)

    @inline final def ~/~[R](success: HtmlValidator[_], error: HtmlValidator[R]) =
      validateWith(success, error :: Nil)

    @inline final def ~/~[R](success: HtmlValidator[_], errors: Seq[HtmlValidator[R]]) =
      validateWith(success, errors)

    @inline final def ~/~[R](success: HtmlValidator[_], error: HtmlValidator[R], default: R) =
      validateWith(success, error :: Nil, default)

    @inline final def ~/~[R](success: HtmlValidator[_], errors: Seq[HtmlValidator[R]], default: R) =
      validateWith(success, errors, default)

    @inline final def and = self
  }

  implicit def deepFunctorOps[FA, A1](self: FA)(implicit df: DeepFunctor[FA] { type A = A1 }, conv: A1 => ElementQuery) =
    new ElementsScrapingOps[df.F, df.A](df.asF(self))(df.f, conv)
}

object ScrapingOps extends ScrapingOps
