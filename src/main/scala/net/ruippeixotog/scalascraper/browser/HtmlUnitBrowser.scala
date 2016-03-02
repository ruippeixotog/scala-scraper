package net.ruippeixotog.scalascraper.browser

import java.io.File
import java.net.URL
import java.util.UUID

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.{ DomElement, HTMLParser, HtmlPage }
import com.gargoylesoftware.htmlunit.util.NameValuePair
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser._
import net.ruippeixotog.scalascraper.model._

class HtmlUnitBrowser extends Browser {
  val client = new WebClient(BrowserVersion.CHROME)
  client.getOptions.setThrowExceptionOnScriptError(false)

  private[this] def newRequest(url: String, method: HttpMethod = HttpMethod.GET) =
    new WebRequest(new URL(url), method)

  private[this] def newWindow(): WebWindow =
    client.openTargetWindow(client.getCurrentWindow, null, UUID.randomUUID().toString)

  private[this] def openInNewWindow(req: WebRequest): HtmlUnitDocument =
    HtmlUnitDocument(client.getPage(newWindow(), req))

  def get(url: String) = openInNewWindow(newRequest(url))

  def post(url: String, form: Map[String, String]) = {
    val req = newRequest(url, HttpMethod.POST)
    req.setRequestParameters(form.map { case (k, v) => new NameValuePair(k, v) }.toSeq)
    openInNewWindow(req)
  }

  def parseFile(path: String, charset: String) =
    openInNewWindow(newRequest(s"file://$path"))

  def parseFile(file: File) =
    openInNewWindow(newRequest(s"file://${file.getAbsolutePath}"))

  def parseFile(file: File, charset: String) =
    openInNewWindow(newRequest(s"file://${file.getAbsolutePath}"))

  def parseString(html: String) = {
    val response = new StringWebResponse(html, WebClient.URL_ABOUT_BLANK)
    HtmlUnitDocument(HTMLParser.parseHtml(response, newWindow()))
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
    def text = underlying.getTextContent

    def innerHtml = underlying.getChildElements.map(_.asXml).mkString
    def outerHtml = underlying.asXml

    private[this] def selectUnderlying(cssQuery: String): Iterator[Element] =
      underlying.querySelectorAll(cssQuery).iterator.collect { case elem: DomElement => HtmlUnitElement(elem) }

    def select(cssQuery: String) = ElementQuery(cssQuery, this, selectUnderlying _)
  }

  case class HtmlUnitDocument(underlying: HtmlPage) extends Document {
    def location = underlying.getDocumentURI
    def root = HtmlUnitElement(underlying.getDocumentElement)

    override def title = underlying.getTitleText
    override def head = HtmlUnitElement(underlying.getHead)
    override def body = HtmlUnitElement(underlying.getBody)

    def toHtml = underlying.asXml
  }
}
