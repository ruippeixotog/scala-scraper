package net.ruippeixotog.scalascraper

import java.io.PrintStream

import com.typesafe.config.ConfigFactory
import net.ruippeixotog.scalascraper.ExampleMatchers._
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import net.ruippeixotog.scalascraper.util.Validated._
import org.jsoup.nodes.Element

import scala.collection.convert.WrapAsScala._
import scala.collection.immutable.SortedMap

object ExampleMatchers {
  val succ = matcherAt[Int]("success-matcher")
  val errs = matchersAt[Int]("error-matchers")
}

object NewsApp extends App {
  val browser = new Browser
  val doc = browser.get("http://observador.pt")

  println()
  println("=== OBSERVADOR ===")

  doc >> extractor(".logo img", attr("src")) |> println
  doc >> extractorAt[String]("example-extractor") |> println

  println("==================")
  println()

  doc >> ".small-news-list h4 > a" foreach println
}

object HeadlineApp extends App {
  val browser = new Browser

  browser.get("http://observador.pt") ~/~ (succ, errs) >> "h1" match {
    case VSuccess(headline) => println("HEADLINE: " + headline.head)
    case VFailure(status) => println("Error: " + status)
  }
}

object HeadlineBetterApp extends App {
  val browser = new Browser

  for {
    headline <- browser.get("http://observador.pt") ~/~ (succ, errs) >> element("h1 a")
    headlineDesc = browser.get(headline.attr("href")) >> text(".lead")
  } println("== " + headline.text + " ==\n" + headlineDesc)
}

object HeadlineVerboseApp extends App {
  val conf = ConfigFactory.load

  val browser = new Browser

  for {
    headline <- browser.get("http://observador.pt") validateWith (succ, errs) extract element("h1 a")
    headlineDesc = browser.get(headline.attr("href")) extract text(".lead")
  } println("== " + headline.text + " ==\n" + headlineDesc)
}

object MusicGenreTreeApp extends App {
  val browser = new Browser

  case class GenreNode(root: Element) {
    def leaves = root >> elements("> a.genre") map { e => e.text -> e }
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
