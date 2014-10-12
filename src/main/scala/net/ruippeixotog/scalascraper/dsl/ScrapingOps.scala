package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.scraper.{HtmlExtractor, HtmlStatusMatcher}
import net.ruippeixotog.scalascraper.util.Validated.{VFailure, VSuccess}
import net.ruippeixotog.scalascraper.util._
import org.jsoup.select.Elements

import scala.util.Try

trait ScrapingOps extends MappableInstances {

  class ElementsScrapingOps[F[_], A <% Elements, FA <% Mappable[F, A]](val self: FA) {

    def extract[B](extractor: HtmlExtractor[B]) = self.mmap(extractor.extract(_))

    @inline final def apply[B](extractor: HtmlExtractor[B]) = extract(extractor)

    @inline final def >>[B](extractor: HtmlExtractor[B]) = extract(extractor)

    @inline final def >>[B, C](extractor1: HtmlExtractor[B], extractor2: HtmlExtractor[C]) =
      (extract(extractor1), extract(extractor2))

    def tryExtract[B](extractor: HtmlExtractor[B]) =
      self.mmap { doc => Try(extractor.extract(doc)).toOption }

    @inline final def tryApply[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    @inline final def ?>>[B](extractor: HtmlExtractor[B]) = tryExtract(extractor)

    def errorIf[R](errors: Seq[HtmlStatusMatcher[R]]) = self.mmap { doc =>
      errors.foldLeft(VSuccess[A, R](doc)) { (res, error) =>
        if(res.isRight || !error.matches(doc)) res else VFailure(error.result)
      }
    }

    def validateWith[R](success: HtmlStatusMatcher[_],
                        errors: Seq[HtmlStatusMatcher[R]],
                        default: => R = throw new Exception("Unknown error matching document")) =
      self.mmap { doc =>
        if(success.matches(doc)) VSuccess(doc)
        else errors.foldLeft(VSuccess[A, R](doc)) { (res, error) =>
          if(res.isRight || !error.matches(doc)) res else VFailure(error.result)
        }.fold(_ => VFailure(default), VFailure.apply)
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

  implicit def elementsScrapingOps[F[_], A <% Elements, FA <% Mappable[F, A]](self: FA) =
    new ElementsScrapingOps[F, A, FA](self)

  // TODO: this explicit delcaration shouldn't be needed
  implicit final def validatedScrapingOps[A <% Elements, R](doc: Validated[A, R]) =
    new ElementsScrapingOps[({ type F[X] = Validated[X, R] })#F, A, Validated[A, R]](doc)

  implicit def functionApplyOps[A](obj: A) = new {
    def |>[B](f: A => B) = f(obj)
  }
}

object ScrapingOps extends ScrapingOps
