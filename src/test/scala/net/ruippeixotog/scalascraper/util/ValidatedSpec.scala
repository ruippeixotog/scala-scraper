package net.ruippeixotog.scalascraper.util

import net.ruippeixotog.scalascraper.util.Validated._
import org.specs2.mutable.Specification

class ValidatedSpec extends Specification {

  "A Validated" should {

    "have correct constructors and extractors" in {

      val succ1: Validated[String, Int] = Validated.success(3)
      val fail1: Validated[String, Int] = Validated.failure[String, Int]("error")
      val succ2: Validated[Exception, String] = VSuccess("yey")
      val fail2: Validated[Exception, String] = VFailure(new Exception("error"))

      succ1 match {
        case VSuccess(res) => res mustEqual 3
        case VFailure(_) => ko
      }

      fail1 match {
        case VSuccess(_) => ko
        case VFailure(msg) => msg mustEqual "error"
      }

      succ2 match {
        case VSuccess(res) => res mustEqual "yey"
        case VFailure(_) => ko
      }

      fail2 match {
        case VSuccess(_) => ko
        case VFailure(ex) => ex.getMessage mustEqual "error"
      }
    }
  }
}
