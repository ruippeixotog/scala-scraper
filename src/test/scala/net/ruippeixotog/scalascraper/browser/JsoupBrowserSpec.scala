package net.ruippeixotog.scalascraper.browser

import java.io.File
import java.net.URL

import org.jsoup.Connection
import org.jsoup.Connection._
import org.jsoup.parser.Parser
import org.specs2.mutable.Specification
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

// TODO missing tests for HTTP request execution
class JsoupBrowserSpec extends Specification {

  "A JsoupBrowser" should {

    case class MockResponse(
        url: URL,
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

    class MockJsoupBrowser(userAgent: String = "jsoup/1.8") extends JsoupBrowser(userAgent) {
      val executedRequests = mutable.ListBuffer.empty[Request]
      val mockResponses = mutable.Map.empty[URL, Response]

      def addMockResponse(res: Response) = mockResponses += res.url -> res

      override def executeRequest(conn: Connection) = {
        val req = conn.request
        executedRequests += req
        mockResponses.getOrElse(req.url, MockResponse(req.url))
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
      val body = JsoupBrowser().parseString(html).body

      body.tagName mustEqual "body"
      body.children.size mustEqual 1

      val div = body.children.head
      div.tagName mustEqual "div"
      div.attr("id") mustEqual "a1"
      div.children.size mustEqual 2
    }

    "parse correctly HTML from a file" in {
      val file = new File(getClass.getClassLoader.getResource("test.html").toURI)
      val body = JsoupBrowser().parseFile(file).body

      body.tagName mustEqual "body"
      body.children.size mustEqual 1

      val div = body.children.head
      div.tagName mustEqual "div"
      div.attr("id") mustEqual "a1"
      div.children.size mustEqual 2
    }

    "execute requests with the specified User-Agent" in {
      val browser = new MockJsoupBrowser("test-agent")
      browser.addMockResponse(MockResponse("http://example.com"))
      browser.get("http://example.com")
      browser.executedRequests.headOption must beSome.which(
        _.header("User-Agent") mustEqual "test-agent")
    }

    "follow redirects specified in 'Location' headers" in {
      val browser = new MockJsoupBrowser()

      browser.addMockResponse(MockResponse(
        "http://example.com/original",
        headerMap = Map("Location" -> "http://example.com/redirected")))

      browser.addMockResponse(MockResponse("http://example.com/redirected", body = html))

      browser.get("http://example.com/original").body.attr("id") mustEqual "bid"
    }

    "keep and use cookies between requests" in {
      val browser = new MockJsoupBrowser()

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
