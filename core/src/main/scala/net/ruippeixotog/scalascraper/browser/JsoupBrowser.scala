package net.ruippeixotog.scalascraper.browser

import java.io.{ File, InputStream }

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.jsoup.Connection.Method._
import org.jsoup.Connection.Response
import org.jsoup.{ Connection, Jsoup }

import net.ruippeixotog.scalascraper.browser.JsoupBrowser._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.util._

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
  type DocumentType = JsoupDocument

  private[this] val cookieMap = mutable.Map.empty[String, String]

  def get(url: String): JsoupDocument =
    executePipeline(Jsoup.connect(url).method(GET))

  def post(url: String, form: Map[String, String]): JsoupDocument =
    executePipeline(Jsoup.connect(url).method(POST).data(form.asJava))

  def parseFile(file: File, charset: String): JsoupDocument =
    JsoupDocument(Jsoup.parse(file, charset))

  def parseString(html: String): JsoupDocument =
    JsoupDocument(Jsoup.parse(html))

  def parseInputStream(inputStream: InputStream, charset: String): JsoupDocument =
    using(inputStream) { _ => JsoupDocument(Jsoup.parse(inputStream, charset, "")) }

  def cookies(url: String) = cookieMap.toMap
 
  def setCookies(m: Map[String, String], url: String) = {
    cookieMap = mutable.Map(m.toSeq: _*)
  }

  def clearCookies() = cookieMap.clear()

  def requestSettings(conn: Connection): Connection = conn

  protected[this] def defaultRequestSettings(conn: Connection): Connection =
    conn.cookies(cookieMap.asJava).
      userAgent(userAgent).
      header("Accept", "text/html,application/xhtml+xml,application/xml").
      header("Accept-Charset", "utf-8").
      timeout(15000).
      maxBodySize(0)

  protected[this] def executeRequest(conn: Connection): Response =
    conn.execute()

  protected[this] def processResponse(res: Connection.Response): JsoupDocument = {
    lazy val doc = res.parse
    cookieMap ++= res.cookies.asScala
    if (res.hasHeader("Location")) get(res.header("Location")) else JsoupDocument(doc)
  }

  private[this] val executePipeline: Connection => JsoupDocument =
    (defaultRequestSettings _)
      .andThen(requestSettings)
      .andThen(executeRequest)
      .andThen(processResponse)
}

object JsoupBrowser {
  def apply(): Browser = new JsoupBrowser()
  def typed(): JsoupBrowser = new JsoupBrowser()

  case class JsoupElement(underlying: org.jsoup.nodes.Element) extends Element {
    type ThisType = JsoupElement

    def tagName = underlying.tagName
    def parent = Option(underlying.parent).map(JsoupElement)
    def children = underlying.children.asScala.map(JsoupElement)
    def siblings = underlying.siblingElements.asScala.map(JsoupElement)

    def childNodes = underlying.childNodes.asScala.flatMap(JsoupNode.apply)
    def siblingNodes = underlying.siblingNodes.asScala.flatMap(JsoupNode.apply)

    def attrs = underlying.attributes.asScala.map { attr => attr.getKey -> attr.getValue }.toMap

    def hasAttr(name: String) = underlying.hasAttr(name)

    def attr(name: String) = {
      if (underlying.hasAttr(name)) underlying.attr(name)
      else throw new NoSuchElementException
    }

    def text = underlying.text

    def innerHtml = underlying.html
    def outerHtml = underlying.outerHtml

    private[this] def selectUnderlying(cssQuery: String): Iterator[JsoupElement] =
      underlying.select(cssQuery).iterator.asScala.map(JsoupElement)

    def select(cssQuery: String) = ElementQuery(cssQuery, this, selectUnderlying)
  }

  object JsoupNode {
    def apply(underlying: org.jsoup.nodes.Node): Option[Node] = underlying match {
      case elem: org.jsoup.nodes.Element => Some(ElementNode(JsoupElement(elem)))
      case textNode: org.jsoup.nodes.TextNode => Some(TextNode(textNode.text))
      case _ => None
    }
  }

  case class JsoupDocument(underlying: org.jsoup.nodes.Document) extends Document {
    type ElementType = JsoupElement

    def location = underlying.location()
    def root = JsoupElement(underlying.getElementsByTag("html").first)

    override def title = underlying.title
    override def head = JsoupElement(underlying.head)
    override def body = JsoupElement(underlying.body)

    def toHtml = underlying.outerHtml
  }
}
