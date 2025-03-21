package net.ruippeixotog.scalascraper.browser

import java.io.{File, InputStream}
import java.net.{InetSocketAddress, Proxy => JavaProxy}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

import org.jsoup.Connection.Method._
import org.jsoup.Connection.Response
import org.jsoup.parser.Parser
import org.jsoup.{Connection, Jsoup}

import net.ruippeixotog.scalascraper.browser.JsoupBrowser._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.util._

/** A [[Browser]] implementation based on [[http://jsoup.org jsoup]], a Java HTML parser library. `JsoupBrowser`
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
  * @param userAgent
  *   the user agent with which requests should be made
  * @param proxy
  *   an optional proxy configuration to use
  */
class JsoupBrowser(
    val userAgent: String = "jsoup/1.8",
    val proxy: JavaProxy = null,
    val parserBuilder: () => Parser = () => Parser.htmlParser()
) extends Browser {
  type DocumentType = JsoupDocument

  private val cookieMap = mutable.Map.empty[String, String]

  def get(url: String): JsoupDocument =
    executePipeline(Jsoup.connect(url).method(GET).proxy(proxy))

  def post(url: String, form: Map[String, String]): JsoupDocument =
    executePipeline(Jsoup.connect(url).method(POST).proxy(proxy).data(form.asJava))

  def parseFile(file: File, charset: String): JsoupDocument =
    JsoupDocument(Jsoup.parse(file, charset, file.getAbsolutePath, parser))

  def parseString(html: String): JsoupDocument =
    JsoupDocument(Jsoup.parse(html, baseUri, parser))

  def parseInputStream(inputStream: InputStream, charset: String): JsoupDocument =
    using(inputStream) { _ => JsoupDocument(Jsoup.parse(inputStream, charset, baseUri, parser)) }

  def cookies(url: String): Map[String, String] = cookieMap.toMap

  def setCookie(url: String, key: String, value: String): mutable.Map[String, String] = {
    cookieMap += key -> value
  }

  def setCookies(url: String, m: Map[String, String]): mutable.Map[String, String] = {
    cookieMap ++= m
  }

  def clearCookies(): Unit = cookieMap.clear()

  def withProxy(proxy: Proxy): JsoupBrowser = {
    val newJavaProxy = new JavaProxy(
      if (proxy.proxyType == Proxy.SOCKS) JavaProxy.Type.SOCKS else JavaProxy.Type.HTTP,
      new InetSocketAddress(proxy.host, proxy.port)
    )
    new JsoupBrowser(userAgent, newJavaProxy)
  }

  private def requestSettings(conn: Connection): Connection = conn

  protected def defaultRequestSettings(conn: Connection): Connection =
    conn
      .cookies(cookieMap.asJava)
      .userAgent(userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml")
      .header("Accept-Charset", "utf-8")
      .timeout(15000)
      .maxBodySize(0)

  protected def executeRequest(conn: Connection): Response =
    conn.execute()

  protected def processResponse(res: Connection.Response): JsoupDocument = {
    lazy val doc = res.parse
    cookieMap ++= res.cookies.asScala
    if (res.hasHeader("Location")) get(res.header("Location")) else JsoupDocument(doc)
  }

  private val executePipeline: Connection => JsoupDocument =
    defaultRequestSettings
      .andThen(requestSettings)
      .andThen(executeRequest)
      .andThen(processResponse)
}

object JsoupBrowser {
  private val baseUri = ""

  def apply(): Browser = new JsoupBrowser()

  def typed(): JsoupBrowser = new JsoupBrowser()

  case class JsoupElement(underlying: org.jsoup.nodes.Element) extends Element {
    type ThisType = JsoupElement

    def tagName: String = underlying.tagName

    def parent: Option[JsoupElement] =
      Option(underlying.parent).map(JsoupElement.apply)

    def children: Iterable[JsoupElement] =
      underlying.children.asScala.map(JsoupElement.apply)

    def siblings: Iterable[JsoupElement] =
      underlying.siblingElements.asScala.map(JsoupElement.apply)

    def childNodes: Iterable[Node] =
      underlying.childNodes.asScala.flatMap(JsoupNode.apply)

    def siblingNodes: Iterable[Node] =
      underlying.siblingNodes.asScala.flatMap(JsoupNode.apply)

    def attrs: Map[String, String] =
      underlying.attributes.asScala.map { attr => attr.getKey -> attr.getValue }.toMap

    def hasAttr(name: String): Boolean = underlying.hasAttr(name)

    def attr(name: String): String = {
      if (underlying.hasAttr(name)) underlying.attr(name)
      else throw new NoSuchElementException
    }

    def text: String = underlying.text

    def ownText: String = underlying.ownText

    def innerHtml: String = underlying.html

    def outerHtml: String = underlying.outerHtml

    private def selectUnderlying(cssQuery: String): Iterator[JsoupElement] =
      underlying.select(cssQuery).iterator.asScala.map(JsoupElement.apply)

    def select(cssQuery: String): ElementQuery[JsoupElement] = ElementQuery(cssQuery, this, selectUnderlying)
  }

  object JsoupNode {
    def apply(underlying: org.jsoup.nodes.Node): Option[Node] =
      underlying match {
        case elem: org.jsoup.nodes.Element => Some(ElementNode(JsoupElement(elem)))
        case textNode: org.jsoup.nodes.TextNode => Some(TextNode(textNode.text))
        case _ => None
      }
  }

  case class JsoupDocument(underlying: org.jsoup.nodes.Document) extends Document {
    type ElementType = JsoupElement

    def location: String = underlying.location()

    def root: JsoupElement = JsoupElement(underlying.getElementsByTag("html").first)

    override def title: String = underlying.title

    override def head: JsoupElement = JsoupElement(underlying.head)

    override def body: JsoupElement = JsoupElement(underlying.body)

    def toHtml: String = underlying.outerHtml
  }
}
