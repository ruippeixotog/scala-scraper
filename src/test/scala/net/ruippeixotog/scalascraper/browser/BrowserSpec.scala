package net.ruippeixotog.scalascraper.browser

import java.io.File
import java.net.URL

import org.jsoup.Connection
import org.jsoup.Connection.{ Method, Response }
import org.jsoup.parser.Parser
import org.specs2.mutable.Specification
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

// TODO missing tests for HTTP request execution
class BrowserSpec extends Specification {

  "A Browser" should {

    case class MockResponse(url: URL,
                            method: Method = Method.GET,
                            statusCode: Int = 200,
                            charset: String = "UTF-8",
                            contentType: String = "text/html",
                            body: String = "",
                            cookieMap: Map[String, String] = Map.empty,
                            headerMap: Map[String, String] = Map.empty) extends Response {

      def statusMessage() = body
      def bodyAsBytes() = body.getBytes

      def parse() = Parser.htmlParser.parseInput(body, url.toString)

      def url(url: URL) = copy(url = url)
      def method(method: Method) = copy(method = method)

      def header(name: String) = headerMap(name)
      def header(name: String, value: String) = copy(headerMap = headerMap + (name -> value))
      def headers() = headerMap
      def hasHeader(name: String) = headerMap.contains(name)
      def hasHeaderWithValue(name: String, value: String) = headerMap.get(name).contains(value)
      def removeHeader(name: String) = copy(headerMap = headerMap - name)

      def cookie(name: String) = cookieMap(name)
      def cookie(name: String, value: String) = copy(cookieMap = cookieMap + (name -> value))
      def cookies() = cookieMap
      def hasCookie(name: String) = cookieMap.contains(name)
      def removeCookie(name: String) = copy(cookieMap = cookieMap - name)
    }

    implicit def stringAsUrl(str: String) = new URL(str)

    class MockBrowser extends Browser {
      val mockResponses = mutable.Map.empty[URL, Response]
      def addMockResponse(res: Response) = mockResponses += res.url -> res

      override def executeRequest(conn: Connection) = {
        val url = conn.request.url
        mockResponses.getOrElse(url, MockResponse(url))
      }
    }

    val html = """
      <html>
        <body id="bid">
          <div id="a1">
            <a></a>
            <div></div>
          </div>
        </body>
      <html>"""

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

    "follow redirects specified in 'Location' headers" in {
      val browser = new MockBrowser()

      browser.addMockResponse(MockResponse("http://example.com/original",
        headerMap = Map("Location" -> "http://example.com/redirected")))

      browser.addMockResponse(MockResponse("http://example.com/redirected", body = html))

      browser.get("http://example.com/original").body.attr("id") mustEqual "bid"
    }

    "follow redirects specified in meta refresh HTML tags" in {
      val browser = new MockBrowser()
      val redirectHtml =
        """<head><meta http-equiv="refresh" content="0;URL='http://example.com/redirected'" /></head>"""

      browser.addMockResponse(MockResponse("http://example.com/original", body = redirectHtml))
      browser.addMockResponse(MockResponse("http://example.com/redirected", body = html))

      browser.get("http://example.com/original").body.attr("id") mustEqual "bid"
    }

    "ignore redirects in meta refresh HTML tags inside noscripts" in {
      val browser = new MockBrowser()
      val redirectHtml =
        """<head><noscript><meta http-equiv="refresh" content="0;URL='http://example.com/redirected'" /></noscript></head><body id='orig'></body>"""

      browser.addMockResponse(MockResponse("http://example.com/original", body = redirectHtml))
      browser.addMockResponse(MockResponse("http://example.com/redirected", body = html))

      browser.get("http://example.com/original").body.attr("id") mustEqual "orig"
    }

    "keep and use cookies between requests" in {
      val browser = new MockBrowser()

      browser.addMockResponse(MockResponse("http://example.com?id=2", cookieMap = Map("a" -> "4")))
      browser.get("http://example.com?id=2")
      browser.cookies.get("a") must beSome("4")
      browser.cookies.get("b") must beNone

      browser.addMockResponse(MockResponse("http://example.com?id=3", cookieMap = Map("b" -> "5")))
      browser.get("http://example.com?id=3")
      browser.cookies.get("a") must beSome("4")
      browser.cookies.get("b") must beSome("5")
    }
  }
}
