package net.ruippeixotog.scalascraper.browser

import java.io.{File, InputStream}
import java.net.URL
import java.nio.charset.Charset
import java.util.UUID

import scala.collection.JavaConverters._

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.htmlunit._
import org.htmlunit.html._
import org.htmlunit.html.parser.neko.HtmlUnitNekoHtmlParser
import org.htmlunit.util.{NameValuePair, StringUtils, UrlUtils}

import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.util._

/** A [[Browser]] implementation based on [[http://htmlunit.sourceforge.net HtmlUnit]], a GUI-less browser for Java
  * programs. `HtmlUnitBrowser` simulates thoroughly a web browser, executing JavaScript code in the pages besides
  * parsing and modelling its HTML content. It supports several compatibility modes, allowing it to emulate browsers
  * such as Internet Explorer.
  *
  * Both the [[net.ruippeixotog.scalascraper.model.Document]] and the [[net.ruippeixotog.scalascraper.model.Element]]
  * instances obtained from `HtmlUnitBrowser` can be mutated in the background. JavaScript code can at any time change
  * attributes and the content of elements, reflected both in queries to `Document` and on previously stored references
  * to `Element`s. The `Document` instance will always represent the current page in the browser's "window". This means
  * the `Document`'s `location` value can change, together with its root element, in the event of client-side page
  * refreshes or redirections. However, `Element` instances belong to a fixed DOM tree and they stop being meaningful as
  * soon as they are removed from the DOM or a client-side page reload occurs.
  *
  * @param browserType
  *   the browser type and version to simulate
  * @param proxy
  *   an optional proxy configuration to use
  */
class HtmlUnitBrowser(browserType: BrowserVersion = BrowserVersion.CHROME, proxy: Option[ProxyConfig] = None)
    extends Browser {
  type DocumentType = HtmlUnitDocument

  lazy val underlying: WebClient = {
    val c = new WebClient(browserType)
    defaultClientSettings(c)
    c
  }

  def userAgent = browserType.getUserAgent

  def exec(req: WebRequest): HtmlUnitDocument = {
    val window = newWindow()
    underlying.getPage(window, req)
    HtmlUnitDocument(window)
  }

  def get(url: String): HtmlUnitDocument =
    exec(newRequest(new URL(url)))

  def post(url: String, form: Map[String, String]): HtmlUnitDocument = {
    val req = newRequest(new URL(url), HttpMethod.POST)
    req.setRequestParameters(form.map { case (k, v) => new NameValuePair(k, v) }.toBuffer.asJava)
    exec(req)
  }

  def parseFile(file: File, charset: String): HtmlUnitDocument = {
    val req = newRequest(new URL(s"file://${file.getAbsolutePath}"), HttpMethod.GET)
    req.setCharset(Charset.forName(charset))
    exec(req)
  }

  def parseString(html: String): HtmlUnitDocument = {
    val response = new StringWebResponse(html, UrlUtils.URL_ABOUT_BLANK)
    val window = newWindow()
    new DefaultPageCreator().createPage(response, window)
    HtmlUnitDocument(window)
  }

  def parseInputStream(inputStream: InputStream, charset: String): HtmlUnitDocument = {
    using(inputStream) { _ =>
      val response = new WebResponse(
        newWebResponseData(inputStream, charset),
        newRequest(UrlUtils.URL_ABOUT_BLANK, charset = Some(charset)),
        0
      )
      val window = newWindow()
      new DefaultPageCreator().createPage(response, window)
      HtmlUnitDocument(window)
    }
  }

  def cookies(url: String) =
    underlying.getCookies(new URL(url)).asScala.map { c => c.getName -> c.getValue }.toMap

  def clearCookies() = underlying.getCookieManager.clearCookies()

  /** Closes all windows opened in this browser.
    */
  def closeAll() = underlying.close()

  def withProxy(proxy: Proxy): HtmlUnitBrowser = {
    val (scheme, isSocks) = proxy.proxyType match {
      case Proxy.HTTP => ("http", false)
      case Proxy.SOCKS => (null, true)
    }
    val newProxyConf = new ProxyConfig(proxy.host, proxy.port, scheme, isSocks)
    new HtmlUnitBrowser(browserType, Some(newProxyConf))
  }

  protected[this] def defaultClientSettings(client: WebClient): Unit = {
    client.getOptions.setCssEnabled(false)
    client.getOptions.setThrowExceptionOnScriptError(false)
    proxy.foreach { proxy => client.getOptions.setProxyConfig(proxy) }
  }

  protected[this] def defaultRequestSettings(req: WebRequest): Unit = {
    req.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml")
    req.setAdditionalHeader("Accept-Charset", "utf-8")
  }

  private[this] def newWebResponseData(inputStream: InputStream, charset: String): WebResponseData = {
    val bytes = IOUtils.toByteArray(inputStream)
    val compiledHeaders = List(new NameValuePair("Content-Type", "text/html; charset=" + charset))
    new WebResponseData(bytes, HttpStatus.SC_OK, "OK", compiledHeaders.asJava)
  }

  private[this] def newRequest(url: URL, method: HttpMethod = HttpMethod.GET, charset: Option[String] = None) = {
    val req = new WebRequest(url, method)
    charset.map(Charset.forName).foreach(req.setCharset)
    defaultRequestSettings(req)
    req
  }

  private[this] def newWindow(): WebWindow =
    underlying.synchronized {
      underlying.openTargetWindow(underlying.getCurrentWindow, null, UUID.randomUUID().toString)
    }
}

object HtmlUnitBrowser {
  def apply(): Browser = new HtmlUnitBrowser()

  def typed(): HtmlUnitBrowser = new HtmlUnitBrowser()

  case class HtmlUnitElement(underlying: DomElement) extends Element {
    type ThisType = HtmlUnitElement

    def tagName: String = underlying.getTagName

    def parent: Option[HtmlUnitElement] =
      Option(underlying.getParentNode).collect { case elem: DomElement => HtmlUnitElement(elem) }

    def children: Iterable[HtmlUnitElement] =
      underlying.getChildElements.asScala.map(HtmlUnitElement.apply)

    def siblings: Iterable[HtmlUnitElement] = {
      val previousSiblings = Stream.iterate(underlying)(_.getPreviousElementSibling).tail.takeWhile(_ != null)
      val nextSiblings = Stream.iterate(underlying)(_.getNextElementSibling).tail.takeWhile(_ != null)
      (previousSiblings.reverse ++ nextSiblings).map(HtmlUnitElement.apply)
    }

    def childNodes: Iterable[Node] =
      underlying.getChildNodes.asScala.flatMap(HtmlUnitNode.apply)

    def siblingNodes: Iterable[Node] = {
      val previousSiblings = Stream.iterate[DomNode](underlying)(_.getPreviousSibling).tail.takeWhile(_ != null)
      val nextSiblings = Stream.iterate[DomNode](underlying)(_.getNextSibling).tail.takeWhile(_ != null)
      (previousSiblings.reverse ++ nextSiblings).flatMap(HtmlUnitNode.apply)
    }

    def attrs: Map[String, String] =
      underlying.getAttributesMap.asScala.mapValues(_.getValue).toMap

    def hasAttr(name: String): Boolean =
      underlying.hasAttribute(name) &&
        (underlying.getAttribute(name) ne DomElement.ATTRIBUTE_NOT_DEFINED)

    def attr(name: String): String = {
      val v = underlying.getAttribute(name)
      if (v ne DomElement.ATTRIBUTE_NOT_DEFINED) v else throw new NoSuchElementException
    }

    def text: String = underlying.getTextContent.trim

    def ownText: String =
      underlying.getChildren.asScala.collect { case node: DomText => node.getWholeText }.mkString

    def innerHtml: String =
      underlying.getChildNodes.iterator.asScala.map {
        case node: DomElement => HtmlUnitElement(node).outerHtml
        case node: DomText => node.getWholeText
        case node => node.asXml.trim
      }.mkString

    def outerHtml: String = {
      val a = attrs.map { case (k, v) => s"""$k="${StringUtils.escapeXmlAttributeValue(v)}"""" }
      val attrsStr = if (a.isEmpty) "" else a.mkString(" ", " ", "")

      s"<$tagName$attrsStr>$innerHtml</$tagName>"
    }

    private[this] def selectUnderlying(cssQuery: String): Iterator[HtmlUnitElement] =
      underlying.querySelectorAll(cssQuery).iterator.asScala.collect { case elem: DomElement => HtmlUnitElement(elem) }

    def select(cssQuery: String): ElementQuery[HtmlUnitElement] =
      ElementQuery(cssQuery, this, selectUnderlying)
  }

  object HtmlUnitNode {
    def apply(underlying: DomNode): Option[Node] =
      underlying match {
        case elem: DomElement => Some(ElementNode(HtmlUnitElement(elem)))
        case textNode: DomText => Some(TextNode(textNode.getWholeText))
        case _ => None
      }
  }

  case class HtmlUnitDocument(window: WebWindow) extends Document {
    type ElementType = HtmlUnitElement

    private[this] var _underlying: SgmlPage = _

    def underlying: SgmlPage = {
      if (_underlying == null || window.getEnclosedPage.getUrl != _underlying.getUrl) {
        _underlying = window.getEnclosedPage match {
          case page: SgmlPage => page
          case page: TextPage =>
            val response = new StringWebResponse(page.getContent, page.getUrl)
            new DefaultPageCreator().createPage(response, window).asInstanceOf[SgmlPage]
        }
      }
      _underlying
    }

    def location = underlying.getUrl.toString

    def root = HtmlUnitElement(underlying.getDocumentElement)

    override def title =
      underlying match {
        case page: HtmlPage => page.getTitleText
        case _ => ""
      }

    def toHtml = root.outerHtml
  }

}
