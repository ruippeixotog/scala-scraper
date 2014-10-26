package net.ruippeixotog.scalascraper.browser

import org.jsoup.Connection.Method._
import org.jsoup.nodes.Document
import org.jsoup.{Connection, Jsoup}

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.{Map => MutableMap}

class Browser {
  val cookies = MutableMap.empty[String, String]

  def get(url: String) =
    executePipeline(Jsoup.connect(url).method(GET))

  def post(url: String, form: Map[String, String]) =
    executePipeline(Jsoup.connect(url).method(POST).data(form))

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
      if(res.hasHeader("Location")) Some(res.header("Location"))
      else doc.select("head meta[http-equiv=refresh]").headOption.map { e =>
        e.attr("content").split(";").find(_.startsWith("url")).head.split("=")(1)
      }

    redirectUrl match {
      case None => doc
      case Some(url) => get(url)
    }
  }
}
