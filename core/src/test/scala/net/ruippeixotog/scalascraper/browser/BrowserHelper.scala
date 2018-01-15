package net.ruippeixotog.scalascraper.browser

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment

trait BrowserHelper { this: Specification =>

  def usingBrowsers(browsers: Browser*)(block: Browser => Fragment): Fragment = {
    browsers.map { browser =>
      s"using ${browser.getClass.getSimpleName}" in {
        block(browser)
      }
    }.last
  }

  def usingBrowsersLazy(browsers: (String, () => Browser)*)(block: (() => Browser) => Fragment): Fragment = {
    browsers.map {
      case (browserName, browser) => s"using $browserName" in { block(browser) }
    }.last
  }
}
