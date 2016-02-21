package net.ruippeixotog.scalascraper.model

trait Element {
  def tagName: String

  def parent: Option[Element]
  def children: Iterable[Element]

  def attrs: Map[String, String]
  def attr(name: String): String

  def text: String
  def innerHtml: String
  def outerHtml: String

  def select(query: String): ElementQuery
}
