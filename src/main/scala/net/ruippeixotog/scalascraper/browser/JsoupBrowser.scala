package net.ruippeixotog.scalascraper.browser

import java.io.{ File, InputStream }

import net.ruippeixotog.scalascraper.browser.JsoupBrowser._
import net.ruippeixotog.scalascraper.model.{ ElementQuery, Document, Element }
import org.jsoup.Connection.Method._
import org.jsoup.Connection.Response
import org.jsoup.{ Connection, Jsoup }

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

/**
  * A [[Browser]] implementation based on [[http://jsoup.org jsoup]], a Java HTML parser library. `JsoupBrowser`
  * provides powerful and efficient document querying, but it doesn't run JavaScript in the pages. As such, it is
  * limited to working strictly with the HTML send in the page source.
  *
  * Currently, `JsoupBrowser` does not keep separate cookie stores for different domains and paths. In each request all
  * cookies set previously will be sent, regardless of the domain they were set on. If you do requests to different
  * domains and do not want this behavior, use different `JsoupBrowser` instances.
  *
  * As the documents parsed by `JsoupBrowser` instances are not changed after loading, `Document` and `Element`
  * instances obtained from them are guaranteed to be immutable.
  *
  * @param userAgent the user agent with which requests should be made
  */
class JsoupBrowser(val userAgent: String = "jsoup/1.8") extends Browser {
  private[this] val cookieMap = mutable.Map.empty[String, String]

  def get(url: String): Document =
    executePipeline(Jsoup.connect(url).method(GET))

  def post(url: String, form: Map[String, String]): Document =
    executePipeline(Jsoup.connect(url).method(POST).data(form))

  def parseFile(file: File, charset: String): Document =
    JsoupDocument(Jsoup.parse(file, charset))

  def parseString(html: String): Document =
    JsoupDocument(Jsoup.parse(html))

  def parseInputStream(inputStream: InputStream, charset: String): Document = {
    val document = JsoupDocument(Jsoup.parse(inputStream, charset, ""))
    inputStream.close()
    document
  }

  def cookies(url: String) = cookieMap.toMap

  def clearCookies() = cookieMap.clear()

  def requestSettings(conn: Connection): Connection = conn

  protected[this] def defaultRequestSettings(conn: Connection): Connection =
    conn.cookies(cookieMap).
      userAgent(userAgent).
      header("Accept", "text/html,application/xhtml+xml,application/xml").
      header("Accept-Charset", "utf-8").
      timeout(15000).
      maxBodySize(0)

  protected[this] def executeRequest(conn: Connection): Response =
    conn.execute()

  protected[this] def processResponse(res: Connection.Response): Document = {
    lazy val doc = res.parse
    cookieMap ++= res.cookies
    if (res.hasHeader("Location")) get(res.header("Location")) else JsoupDocument(doc)
  }

  private[this] val executePipeline: Connection => Document =
    (defaultRequestSettings _)
      .andThen(requestSettings)
      .andThen(executeRequest)
      .andThen(processResponse)
}

object JsoupBrowser {
  def apply(): JsoupBrowser = new JsoupBrowser()

  case class JsoupElement(underlying: org.jsoup.nodes.Element) extends Element {
    def tagName = underlying.tagName
    def parent = Option(underlying.parent).map(JsoupElement)
    def children = underlying.children.toIterable.map(JsoupElement)
    def siblings = underlying.siblingElements.map(JsoupElement)

    def attrs = underlying.attributes.map { attr => attr.getKey -> attr.getValue }.toMap

    def hasAttr(name: String) = underlying.hasAttr(name)

    def attr(name: String) = {
      if (underlying.hasAttr(name)) underlying.attr(name)
      else throw new NoSuchElementException
    }

    def text = underlying.text

    def innerHtml = underlying.html
    def outerHtml = underlying.outerHtml

    private[this] def selectUnderlying(cssQuery: String): Iterator[Element] =
      underlying.select(cssQuery).iterator.map(JsoupElement)

    def select(cssQuery: String) = ElementQuery(cssQuery, this, selectUnderlying)
  }

  case class JsoupDocument(underlying: org.jsoup.nodes.Document) extends Document {
    def location = underlying.location()
    def root = JsoupElement(underlying.getElementsByTag("html").first)

    override def title = underlying.title
    override def head = JsoupElement(underlying.head)
    override def body = JsoupElement(underlying.body)

    def toHtml = underlying.outerHtml
  }
}
