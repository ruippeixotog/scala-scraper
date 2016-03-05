package net.ruippeixotog.scalascraper.browser

import java.io.File

import net.ruippeixotog.scalascraper.browser.JsoupBrowser._
import net.ruippeixotog.scalascraper.model.{ ElementQuery, Document, Element }
import org.jsoup.Connection.Method._
import org.jsoup.Connection.Response
import org.jsoup.{ Connection, Jsoup }

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class JsoupBrowser(userAgent: String = "jsoup/1.8") extends Browser {
  val cookies = mutable.Map.empty[String, String]

  def get(url: String): Document =
    executePipeline(Jsoup.connect(url).method(GET))

  def post(url: String, form: Map[String, String]): Document =
    executePipeline(Jsoup.connect(url).method(POST).data(form))

  def parseFile(path: String, charset: String = "UTF-8"): Document =
    JsoupDocument(Jsoup.parse(new File(path), charset))

  def parseFile(file: File): Document =
    JsoupDocument(Jsoup.parse(file, "UTF-8"))

  def parseFile(file: File, charset: String): Document =
    JsoupDocument(Jsoup.parse(file, charset))

  def parseString(html: String): Document =
    JsoupDocument(Jsoup.parse(html))

  def requestSettings(conn: Connection): Connection = conn

  protected[this] def defaultRequestSettings(conn: Connection): Connection =
    conn.cookies(cookies).
      userAgent(userAgent).
      header("Accept", "text/html,application/xhtml+xml,application/xml").
      header("Accept-Charset", "utf-8").
      timeout(15000).
      maxBodySize(0)

  protected[this] def executeRequest(conn: Connection): Response =
    conn.execute()

  protected[this] def processResponse(res: Connection.Response): Document = {
    lazy val doc = res.parse

    cookies ++= res.cookies.mapValues { v =>
      if (v.head == '"' && v.last == '"') v.substring(1, v.length - 1)
      else v // TODO investigate more thoroughly this parsing problem
    }

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

    def attrs = underlying.attributes.map { attr => attr.getKey -> attr.getValue }.toMap
    def attr(name: String) = underlying.attr(name)
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
