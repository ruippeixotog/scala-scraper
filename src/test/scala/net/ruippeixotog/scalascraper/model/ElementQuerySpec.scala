package net.ruippeixotog.scalascraper.model

import java.io.File
import net.ruippeixotog.scalascraper.browser._
import org.specs2.mutable.Specification

class ElementQuerySpec extends Specification with BrowserHelper {

  "An ElementQuery" should {

    val file = new File(getClass.getClassLoader.getResource("test2.html").toURI)

    usingBrowsers(JsoupBrowser(), HtmlUnitBrowser()) { browser =>
      val doc = browser.parseFile(file)

      "provide the correct results in single CSS queries" in {
        def query(cssQuery: String) = ElementQuery(cssQuery, doc.root).map(_.text)

        query("h3").toSet mustEqual Set("Section 1 h3", "Section 2 h3", "Section 3 h3")
        query("#date, #rating").toSet mustEqual Set("2014-10-26", "4.5")
        query("#content > span").toSet mustEqual Set("2014-10-26", "4.5", "2")
        query("h2 + span").toSet mustEqual Set("2014-10-26")
        query("#content span ~ span").toSet mustEqual Set("4.5", "2")
        query("#content > *:nth-child(3)").toSet mustEqual Set("4.5")
      }

      "allow composing CSS queries" in {
        def query(cssQuery: String*) = cssQuery.foldLeft(ElementQuery(doc.root))(_.select(_)).map(_.text)

        query("#content", "span").toSet mustEqual Set("2014-10-26", "4.5", "2", "My Form")
        query("#menu, #myform", "a").toSet mustEqual Set("Home", "Section 1", "Section 3", "Add field")
        query("#menu, #myform", ".active, *[href=\"#\"]").toSet mustEqual Set("Section 2", "Add field")
        query("section + section", "h3, p").toSet mustEqual Set("Section 2 h3", "Section 3 h3")
        query("body", "section", "span").toSet mustEqual Set("My Form")
      }

      "return the target element if no query was provided" in {
        ElementQuery(doc.root).toSet mustEqual Set(doc.root)
      }

      "have a correct equals method" in {
        val body = doc.root.select("body").head

        ElementQuery(doc.root) mustEqual ElementQuery(doc.root)
        ElementQuery(doc.root) mustNotEqual ElementQuery(body)

        ElementQuery("body", doc.root) mustEqual ElementQuery(body)
        ElementQuery("#date", body) mustEqual ElementQuery("#content > #date", body)
      }

      "have a correct hashCode method" in {
        val body = doc.root.select("body").head

        ElementQuery(doc.root).## mustEqual ElementQuery(doc.root).##
        ElementQuery(doc.root).## mustNotEqual ElementQuery(body).##

        ElementQuery("body", doc.root).## mustEqual ElementQuery(body).##
        ElementQuery("#date", body).## mustEqual ElementQuery("#content > #date", body).##
      }
    }
  }
}
