package net.ruippeixotog.scalascraper.browser

import java.io.File

import org.jsoup.Connection.Method._
import org.jsoup.nodes.Document
import org.jsoup.{ Connection, Jsoup }

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.{ Map => MutableMap }

import Browser._

class Browser {
  val cookies = MutableMap.empty[String, String]

  def get(url: String) =
    executePipeline(Jsoup.connect(url).method(GET))

  def post(url: String, form: Map[String, String]) =
    executePipeline(Jsoup.connect(url).method(POST).data(form))

  def parseFile(path: String, charset: String = "UTF-8") = Jsoup.parse(new File(path), charset)
  def parseFile(file: File) = Jsoup.parse(file, "UTF-8")
  def parseFile(file: File, charset: String) = Jsoup.parse(file, charset)

  def parseString(html: String) = Jsoup.parse(html)

  def requestSettings(conn: Connection): Connection = conn

  private[this] def defaultRequestSettings(conn: Connection) =
    conn.cookies(cookies).
      userAgent("jsoup/1.8.1").
      header("Accept", "text/html,application/xhtml+xml,application/xml").
      header("Accept-Charset", "utf-8").
      timeout(15000).
      maxBodySize(0)

  private[this] val executePipeline: Connection => Document =
    (defaultRequestSettings _).andThen(requestSettings).andThen(process)

  private[this] def process(conn: Connection) = {
    val res = conn.execute()
    lazy val doc = res.parse

    cookies ++= res.cookies

    val redirectUrl =
      if (res.hasHeader("Location")) Some(res.header("Location"))
      else doc.select("head meta[http-equiv=refresh]").headOption.flatMap { e =>
        e.attr("content") match {
          case QuotedMetaRefreshUrl(url) => Some(url)
          case MetaRefreshUrl(url) => Some(url)
          case _ => None
        }
      }

    redirectUrl match {
      case None => doc
      case Some(url) => get(url)
    }
  }
}

object Browser {
  // TODO this is a best effort, the specs define a much more complicated parsing process
  private val QuotedMetaRefreshUrl = ".*URL='([^']+)'.*".r
  private val MetaRefreshUrl = ".*URL=([^;]+).*".r
}
