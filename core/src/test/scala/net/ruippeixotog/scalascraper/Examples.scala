package net.ruippeixotog.scalascraper

import java.io.PrintStream
import java.net.{ InetSocketAddress, Proxy }

import scala.collection.immutable.SortedMap

import net.ruippeixotog.scalascraper.ExampleMatchers._
import net.ruippeixotog.scalascraper.browser.{ HtmlUnitBrowser, JsoupBrowser }
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.HtmlValidator
import net.ruippeixotog.scalascraper.util.EitherRightBias._

object ExampleMatchers {
  val succ = HtmlValidator(text("head > title"), 1)(_.matches(".*Observador.*"))
  val errs = Seq(
    HtmlValidator(attr("content")("meta[name=viewport]"), 2)(_.matches(".*initial-scale=2\\.0.*")),
    HtmlValidator(elements("meta[name=viewport]"), 3)(_.nonEmpty))
}

object ProxyApp extends App {
  val browser = new JsoupBrowser(proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 3128)))
  val doc = browser.get("http://observador.pt")

  println("=== OBSERVADOR HTTP & HTTPS PROXY ===")

  Thread.sleep(2000)

  // You should get a [java.net.SocketTimeoutException: connect timed out] if you are behind a proxy
  browser.get("http://observador.pt")
}

object NewsApp extends App {
  val browser = JsoupBrowser()
  val doc = browser.get("http://observador.pt")

  println()
  println("=== OBSERVADOR ===")

  doc >> extractor(".logo img", attr("src")) |> println
  doc >> extractor("meta[name=description]", attr("content")) |> println

  println("==================")
  println()

  doc >> ".small-news-list h4 > a" foreach println
}

object HeadlineApp extends App {
  val browser = JsoupBrowser()

  browser.get("http://observador.pt") >/~ (succ, errs) >> "h1" match {
    case Right(headline) => println("HEADLINE: " + headline.head)
    case Left(status) => println("Error: " + status)
  }
}

object HeadlineBetterApp extends App {
  val browser = JsoupBrowser()

  for {
    headline <- browser.get("http://observador.pt") >/~ (succ, errs) >> element("h1 a")
    headlineDesc = browser.get(headline.attr("href")) >> text(".lead")
  } println("== " + headline.text + " ==\n" + headlineDesc)
}

object HeadlineVerboseApp extends App {
  val browser = JsoupBrowser()

  for {
    headline <- browser.get("http://observador.pt") validateWith (succ, errs) extract element("h1 a")
    headlineDesc = browser.get(headline.attr("href")) extract text(".lead")
  } println("== " + headline.text + " ==\n" + headlineDesc)
}

object MusicGenreTreeApp extends App {
  val browser = JsoupBrowser()

  case class GenreNode(root: Element) {
    def leaves = root >> elementList("> a.genre") map { e => e.text -> e }
    def nodes = root >> elementList("> div:has(b:has(a.genre))") >> (text(".genre"), element("blockquote"))

    def children: Map[String, GenreNode] = SortedMap(leaves ++ nodes: _*).mapValues(GenreNode.apply)

    def renderYaml(d: Int = 0): String =
      children.map {
        case (k, v) if v.children.isEmpty => s"${" " * d}- $k\n"
        case (k, v) => s"${" " * d}- $k:\n${v.renderYaml(d + 2)}"
      }.mkString
  }

  val page = browser.get("http://rateyourmusic.com/rgenre/")
  val out = new PrintStream("genres.yaml")
  GenreNode(page >> element("#content")).renderYaml() |> out.println
}

object PageInteractionApp extends App {
  val browser = HtmlUnitBrowser.typed()
  val doc = browser.get("http://example.com")

  val moreInfoLink = doc >> pElement("a")
  moreInfoLink.underlying.click()

  doc >> text("h1").map("== " + _ + "==") |> println
  doc >> texts("p") foreach println
}
