package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper.{HtmlExtractor, HtmlStatusMatcher}
import net.ruippeixotog.scalascraper.util.Validated.{VFailure, VSuccess}
import net.ruippeixotog.scalascraper.util._
import org.jsoup.select.Elements

import scala.util.Try
import scalaz._
import scalaz.syntax.functor._

trait ScrapingOps extends syntax.ToIdOps with std.AllInstances with IdInstances {

  class ElementsScrapingOps[+F[_]: Functor, A <% Elements](val self: F[A]) {

    def extract[B](extractor: HtmlExtractor[B]) = self.map(extractor.extract(_))

    @inline final def apply[B](extractor: HtmlExtractor[B]) = extract(extractor)

    @inline final def >>[B](extractor: HtmlExtractor[B]) = extract(extractor)

    @inline final def >>[B, C](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C]) =
      self.map { doc => (extractor1.extract(doc), extractor2.extract(doc)) }

    def tryExtract[B](extractor: HtmlExtractor[B]) =
      self.map { doc => Try(extractor.extract(doc)).toOption }

    @inline final def tryApply[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    @inline final def >?>[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    def errorIf[R](errors: Seq[HtmlStatusMatcher[R]]) = self.map { doc =>
      errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
        if(res.isRight || !error.matches(doc)) res else VFailure(error.result)
      }
    }

    def validateWith[R](success: HtmlStatusMatcher[_],
                        errors: Seq[HtmlStatusMatcher[R]],
                        default: => R = throw new Exception("Unknown error matching document")): F[Validated[R, A]] =
      self.map { doc =>
        if(success.matches(doc)) VSuccess(doc)
        else errors.foldLeft(VSuccess[R, A](doc)) { (res, error) =>
          if(res.isRight || !error.matches(doc)) res else VFailure(error.result)
        }.fold(VFailure.apply, _ => VFailure(default))
      }

    @inline final def ~/~[R](error: HtmlStatusMatcher[R]) = errorIf(error :: Nil)

    @inline final def ~/~[R](errors: Seq[HtmlStatusMatcher[R]]) = errorIf(errors)

    @inline final def ~/~[R](success: HtmlStatusMatcher[_], error: HtmlStatusMatcher[R]) =
      validateWith(success, error :: Nil)

    @inline final def ~/~[R](success: HtmlStatusMatcher[_], errors: Seq[HtmlStatusMatcher[R]]) =
      validateWith(success, errors)

    @inline final def ~/~[R](success: HtmlStatusMatcher[_], error: HtmlStatusMatcher[R], default: R) =
      validateWith(success, error :: Nil, default)

    @inline final def ~/~[R](success: HtmlStatusMatcher[_], errors: Seq[HtmlStatusMatcher[R]], default: R) =
      validateWith(success, errors, default)

    @inline final def and = self
  }

  implicit def elementsScrapingOps[F[_]: Functor, A <% Elements](self: F[A]) =
    new ElementsScrapingOps[F, A](self)

  // TODO: these explicit delcarations shouldn't be needed
  implicit def idScrapingOps[A <% Elements](self: A) = new ElementsScrapingOps[Id, A](self)

  implicit final def validatedScrapingOps[R, A <% Elements](self: Validated[R, A]) =
    new ElementsScrapingOps[({ type F[X] = Validated[R, X] })#F, A](self)

  // TODO: this only handles two levels of functors (not that more levels would be really needed)
  implicit def compositeScrapingOps[F[_]: Functor, G[_]: Functor, A <% Elements](self: F[G[A]]) = {
    implicit val FG = implicitly[Functor[F]].compose[G]
    new ElementsScrapingOps[({ type FG[X] = F[G[X]] })#FG, A](self)
  }
}

object ScrapingOps extends ScrapingOps
