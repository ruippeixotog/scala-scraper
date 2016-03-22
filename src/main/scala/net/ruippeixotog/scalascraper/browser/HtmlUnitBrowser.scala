package net.ruippeixotog.scalascraper.browser

import java.io.File
import java.net.URL
import java.util.UUID

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.{ DomText, DomElement, HTMLParser, HtmlPage }
import com.gargoylesoftware.htmlunit.util.{ StringUtils, NameValuePair }
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.util.ProxyUtils

/**
  * A [[Browser]] implementation based on [[http://htmlunit.sourceforge.net HtmlUnit]], a GUI-less browser for Java
  * programs. `HtmlUnitBrowser` simulates thoroughly a web browser, executing JavaScript code in the pages besides
  * parsing and modelling its HTML content. It supports several compatibility modes, allowing it to emulate browsers
  * such as Internet Explorer.
  *
  * Both the [[Document]] and the [[Element]] instances obtained from `HtmlUnitBrowser` can be mutated in the
  * background. JavaScript code can at any time change attributes and the content of elements, reflected both in queries
  * to `Document` and on previously stored references to `Element`s. The `Document` instance will always represent the
  * current page in the browser's "window". This means the `Document`'s `location` value can change, together with its
  * root element, in the event of client-side page refreshes or redirections. However, `Element` instances belong to a
  * fixed DOM tree and they stop being meaningful as soon as they are removed from the DOM or a client-side page reload
  * occurs.
  *
  * @param browserType the browser type and version to simulate
  */
class HtmlUnitBrowser(browserType: BrowserVersion = BrowserVersion.CHROME) extends Browser {

  private[this] lazy val client = {
    val c = ProxyUtils.getProxy match {
      case Some((proxyHost, proxyPort)) => new WebClient(browserType, proxyHost, proxyPort)
      case None => new WebClient(browserType)
    }
    defaultClientSettings(c)
    c
  }

  def userAgent = browserType.getUserAgent

  def exec(req: WebRequest): Document = {
    val window = newWindow()
    client.getPage(window, req)
    HtmlUnitDocument(window)
  }

  def get(url: String): Document =
    exec(newRequest(url))

  def post(url: String, form: Map[String, String]): Document = {
    val req = newRequest(url, HttpMethod.POST)
    req.setRequestParameters(form.map { case (k, v) => new NameValuePair(k, v) }.toSeq)
    exec(req)
  }

  def parseFile(file: File, charset: String): Document = {
    val req = newRequest(s"file://${file.getAbsolutePath}", HttpMethod.GET)
    req.setCharset(charset)
    exec(req)
  }

  def parseString(html: String): Document = {
    val response = new StringWebResponse(html, WebClient.URL_ABOUT_BLANK)
    val window = newWindow()
    HTMLParser.parseHtml(response, window)
    HtmlUnitDocument(window)
  }

  def cookies(url: String) =
    client.getCookies(new URL(url)).map { c => c.getName -> c.getValue }.toMap

  protected[this] def defaultClientSettings(client: WebClient): Unit = {
    client.getOptions.setCssEnabled(false)
    client.getOptions.setThrowExceptionOnScriptError(false)
  }

  protected[this] def defaultRequestSettings(req: WebRequest): Unit = {
    req.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml")
    req.setAdditionalHeader("Accept-Charset", "utf-8")
  }

  private[this] def newRequest(url: String, method: HttpMethod = HttpMethod.GET) = {
    val req = new WebRequest(new URL(url), method)
    defaultRequestSettings(req)
    req
  }

  private[this] def newWindow(): WebWindow = client.synchronized {
    client.openTargetWindow(client.getCurrentWindow, null, UUID.randomUUID().toString)
  }
}

object HtmlUnitBrowser {
  def apply(): HtmlUnitBrowser = new HtmlUnitBrowser()

  case class HtmlUnitElement(underlying: DomElement) extends Element {
    def tagName = underlying.getTagName
    def parent = Option(underlying.getParentNode).collect { case elem: DomElement => HtmlUnitElement(elem) }
    def children = underlying.getChildElements.map(HtmlUnitElement)

    def attrs = underlying.getAttributesMap.mapValues(_.getValue).toMap
    def attr(name: String) = underlying.getAttribute(name)
    def text = underlying.getTextContent.trim

    def innerHtml = underlying.getChildNodes.iterator.map {
      case node: DomElement => HtmlUnitElement(node).outerHtml
      case node: DomText => node.getWholeText
      case node => node.asXml.trim
    }.mkString

    def outerHtml = {
      val a = attrs.map { case (k, v) => s"""$k="${StringUtils.escapeXmlAttributeValue(v)}"""" }
      val attrsStr = if (a.isEmpty) "" else a.mkString(" ", " ", "")

      s"<$tagName$attrsStr>$innerHtml</$tagName>"
    }

    private[this] def selectUnderlying(cssQuery: String): Iterator[Element] =
      underlying.querySelectorAll(cssQuery).iterator.collect { case elem: DomElement => HtmlUnitElement(elem) }

    def select(cssQuery: String) = ElementQuery(cssQuery, this, selectUnderlying)
  }

  case class HtmlUnitDocument(window: WebWindow) extends Document {
    private[this] var _underlying: SgmlPage = null

    def underlying: SgmlPage = {
      if (_underlying == null || window.getEnclosedPage.getUrl != _underlying.getUrl) {
        _underlying = window.getEnclosedPage match {
          case page: SgmlPage => page
          case page: TextPage =>
            val response = new StringWebResponse(page.getContent, page.getUrl)
            HTMLParser.parseHtml(response, page.getEnclosingWindow)
        }
      }
      _underlying
    }

    def location = underlying.getUrl.toString
    def root = HtmlUnitElement(underlying.getDocumentElement)

    override def title = underlying match {
      case page: HtmlPage => page.getTitleText
      case _ => ""
    }

    def toHtml = root.outerHtml
  }
}
