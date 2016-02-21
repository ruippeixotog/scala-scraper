package net.ruippeixotog.scalascraper.model

trait Document {
  def location: String
  def root: Element

  def title: String = root.select("title").headOption.fold("")(_.text.trim)
  def head: Element = root.select("head").head
  def body: Element = root.select("body").head

  def toHtml: String
}
