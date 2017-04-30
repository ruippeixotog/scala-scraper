package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

object ContentExtractors {

  val element: ElementQuery[Element] => Element = _.head
  val elements: ElementQuery[Element] => ElementQuery[Element] = identity
  val elementList: ElementQuery[Element] => List[Element] = _.toList

  def elementOf[E <: Element.Upper[E]]: ElementQuery[E] => E = _.head
  def elementsOf[E <: Element.Upper[E]]: ElementQuery[E] => ElementQuery[E] = identity
  def elementListOf[E <: Element.Upper[E]]: ElementQuery[E] => List[E] = _.toList

  val text: ElementQuery[Element] => String = _.head.text
  val texts: ElementQuery[Element] => Iterable[String] = _.map(_.text)
  val allText: ElementQuery[Element] => String = _.map(_.text).mkString

  def attr(attr: String): ElementQuery[Element] => String = _.head.attr(attr)
  def attrs(attr: String): ElementQuery[Element] => Iterable[String] = _.map(_.attr(attr))

  // TODO add support for <select> and <textarea> elements
  // TODO add proper support for checkboxes and radio buttons
  // See: https://www.w3.org/TR/html5/forms.html#constructing-form-data-set
  val formData: ElementQuery[Element] => Map[String, String] =
    _.select("input")
      .filter(_.hasAttr("name"))
      .map { e => e.attr("name") -> (if (e.hasAttr("value")) e.attr("value") else "") }
      .toMap

  val formDataAndAction: ElementQuery[Element] => (Map[String, String], String) = { elems =>
    (formData(elems), attr("action")(elems))
  }
}
