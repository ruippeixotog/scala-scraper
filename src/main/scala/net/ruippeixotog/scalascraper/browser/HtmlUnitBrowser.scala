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

class HtmlUnitBrowser(browserType: BrowserVersion = BrowserVersion.CHROME) extends Browser {

  val client = ProxyUtils.getProxy match {
    case Some((proxyHost, proxyPort)) => new WebClient(browserType, proxyHost, proxyPort)
    case None => new WebClient(browserType)
  }

  client.getOptions.setThrowExceptionOnScriptError(false)

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

  def parseFile(path: String, charset: String): Document =
    exec(newRequest(s"file://$path"))

  def parseFile(file: File): Document =
    exec(newRequest(s"file://${file.getAbsolutePath}"))

  def parseFile(file: File, charset: String): Document =
    exec(newRequest(s"file://${file.getAbsolutePath}"))

  def parseString(html: String): Document = {
    val response = new StringWebResponse(html, WebClient.URL_ABOUT_BLANK)
    val window = newWindow()
    HTMLParser.parseHtml(response, window)
    HtmlUnitDocument(window)
  }

  private[this] def newRequest(url: String, method: HttpMethod = HttpMethod.GET) = {
    val req = new WebRequest(new URL(url), method)
    defaultRequestSettings(req)
    req
  }

  protected[this] def defaultRequestSettings(req: WebRequest): Unit = {
    req.setAdditionalHeader("Accept", "text/html,application/xhtml+xml,application/xml")
    req.setAdditionalHeader("Accept-Charset", "utf-8")
  }

  private[this] def newWindow(): WebWindow =
    client.openTargetWindow(client.getCurrentWindow, null, UUID.randomUUID().toString)
}

object HtmlUnitBrowser {
  def apply(): HtmlUnitBrowser = new HtmlUnitBrowser()

  case class HtmlUnitElement(underlying: DomElement) extends Element {
    def tagName = underlying.getTagName
    def parent = Option(underlying.getParentNode).collect { case elem: DomElement => HtmlUnitElement(elem) }
    def children = underlying.getChildElements.map(HtmlUnitElement)

    def attrs = underlying.getAttributesMap.mapValues(_.getValue).toMap
    def attr(name: String) = underlying.getAttribute(name)
    def text = underlying.getTextContent

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
