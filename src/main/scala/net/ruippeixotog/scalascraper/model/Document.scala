package net.ruippeixotog.scalascraper.model

/**
  * A representation of an HTML document.
  *
  * This trait provides methods for retrieving the document's location and the root element, with which further queries
  * can be made. It also has methods for quick retrieval of common information and nodes, such as the title and body of
  * the page.
  *
  * Depending on the type of [[net.ruippeixotog.scalascraper.browser.Browser]] used to load `Document` objects, the
  * respective pages may or may not be dynamic. As such, there are no guarantees of whether the document's location is a
  * constant value and that returned [[Element]] instances will be updated as the DOM nodes are updated. The
  * documentation of each `Browser` implementation should be read for more information on the semantics of its
  * `Document` and `Element` implementations.
  */
trait Document {

  /**
    * The location of this document.
    */
  def location: String

  /**
    * The root element of this document.
    */
  def root: Element

  /**
    * The title of this document.
    */
  def title: String = root.select("title").headOption.fold("")(_.text.trim)

  /**
    * The `head` element of this document.
    */
  def head: Element = root.select("head").head

  /**
    * The `body` element of this document.
    */
  def body: Element = root.select("body").head

  /**
    * The HTML representation of this document as a string.
    */
  def toHtml: String
}
