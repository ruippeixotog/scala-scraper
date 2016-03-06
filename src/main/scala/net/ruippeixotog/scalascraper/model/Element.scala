package net.ruippeixotog.scalascraper.model

/**
  * A representation of a HTML DOM element.
  *
  * Elements can be obtained by obtaining [[Document]] instances (for example, through a
  * [[net.ruippeixotog.scalascraper.browser.Browser]]) and using one of its several methods. They provide several
  * methods for traversing and retrieving infomation from the DOM of the page in which they are.
  */
trait Element {

  /**
    * The tag name of this element.
    */
  def tagName: String

  /**
    * The element of this element.
    */
  def parent: Option[Element]

  /**
    * The list of children of this element.
    */
  def children: Iterable[Element]

  /**
    * The map of attributes of this element.
    */
  def attrs: Map[String, String]

  /**
    * Returns the value associated with an attribute of this element.
    *
    * @param name the name of the attribute
    * @return the value associated with the given attribute.
    */
  def attr(name: String): String

  /**
    * The text content inside this element.
    */
  def text: String

  /**
    * The HTML representation of the content inside this element as a string.
    */
  def innerHtml: String

  /**
    * The HTML representation of this element as a string.
    */
  def outerHtml: String

  /**
    * Executes a query on this element using a CSS selector.
    *
    * @param query the CSS selector used to select elements to be returned
    * @return an `ElementQuery` instance representing the sequence of resulting elements
    */
  def select(query: String): ElementQuery
}
