package net.ruippeixotog.scalascraper.browser

import java.io.File

import org.specs2.mutable.Specification

// TODO missing tests for HTTP request execution
class BrowserSpec extends Specification {

  "A Browser" should {

    val html = """
      <body>
        <div id="a1">
          <a></a>
          <div></div>
        </div>
      </body>"""

    "parse correctly HTML from a string" in {
      val body = new Browser().parseString(html).body

      body.nodeName mustEqual "body"
      body.children.size mustEqual 1

      val div = body.child(0)
      div.nodeName mustEqual "div"
      div.attr("id") mustEqual "a1"
      div.children.size mustEqual 2
    }

    "parse correctly HTML from a file" in {
      val file = new File(getClass.getClassLoader.getResource("test.html").toURI)
      val body = new Browser().parseFile(file).body

      body.nodeName mustEqual "body"
      body.children.size mustEqual 1

      val div = body.child(0)
      div.nodeName mustEqual "div"
      div.attr("id") mustEqual "a1"
      div.children.size mustEqual 2
    }
  }
}
