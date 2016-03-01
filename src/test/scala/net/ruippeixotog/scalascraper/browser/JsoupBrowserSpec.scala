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
        <head id="hid">
          <title>Test Page</title>
        </head>
        <body id="bid" data-a="a" data-b="b">
          <div id="a1">
            <a></a>
            <div></div>
          </div>
          <span></span>
          <span></span>
        </body>
      <html>"""

  "A JsoupBrowser" should {

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

  "A JsoupBrowser's Document" should {

    "have a correct location method" in {
      val browser = new MockJsoupBrowser("test-agent")
      browser.addMockResponse(MockResponse("http://example.com"))
      val doc = browser.get("http://example.com")

      doc.location mustEqual "http://example.com"
    }

    "have correct title, head and body methods" in {
      val browser = new MockJsoupBrowser("test-agent")
      browser.addMockResponse(MockResponse("http://example.com", body = html))
      val doc = browser.get("http://example.com")

      doc.title mustEqual "Test Page"
      doc.head.attr("id") mustEqual "hid"
      doc.body.attr("id") mustEqual "bid"
    }

    "have a correct toHtml method" in {
      val browser = new MockJsoupBrowser("test-agent")
      browser.addMockResponse(MockResponse("http://example.com", body = "<html><head></head><body></body></html>"))
      val doc = browser.get("http://example.com")

      doc.toHtml.filterNot(_.isWhitespace) mustEqual "<html><head></head><body></body></html>"
    }
  }

  "A JsoupBrowser's Element" should {

    "have correct parent and children methods" in {
      val doc = JsoupBrowser().parseString(html)

      val body = doc.body
      body.parent must beSome.which { p => p.tagName mustEqual "html" }
      body.children.map(_.tagName) mustEqual Iterable("div", "span", "span")

      val a = doc.root.select("a").head
      a.parent must beSome.which { p => p.attr("id") mustEqual "a1" }
      a.children must beEmpty
    }

    "have correct attr and attrs methods" in {
      val doc = JsoupBrowser().parseString(html)

      val body = doc.body
      body.attrs mustEqual Map("id" -> "bid", "data-a" -> "a", "data-b" -> "b")
      body.attr("id") mustEqual "bid"
      body.attr("data-b") mustEqual "b"
    }

    "have correct innerHtml and outerHtml methods" in {
      val doc = JsoupBrowser().parseString(html)

      val title = doc.root.select("title").head
      title.innerHtml mustEqual "Test Page"
      title.outerHtml mustEqual "<title>Test Page</title>"
    }
  }
}
