package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

object ContentExtractors {
  @inline private[this] implicit def funcToExtractor[E <: Element, A](f: ElementQuery[E] => A) = HtmlExtractor(f)

  val element: HtmlExtractor[Element, Element] = _.head
  val elements: HtmlExtractor[Element, ElementQuery[Element]] = identity[ElementQuery[Element]](_)
  val elementList: HtmlExtractor[Element, List[Element]] = _.toList

  def elementOf[E <: Element.Upper[E]]: HtmlExtractor[E, E] = _.head
  def elementsOf[E <: Element.Upper[E]]: HtmlExtractor[E, ElementQuery[E]] = identity[ElementQuery[E]](_)
  def elementListOf[E <: Element.Upper[E]]: HtmlExtractor[E, List[E]] = _.toList

  val text: HtmlExtractor[Element, String] = _.head.text
  val texts: HtmlExtractor[Element, Iterable[String]] = _.map(_.text)
  val allText: HtmlExtractor[Element, String] = _.map(_.text).mkString

  def attr(attr: String): HtmlExtractor[Element, String] = _.head.attr(attr)
  def attrs(attr: String): HtmlExtractor[Element, Iterable[String]] = _.map(_.attr(attr))

  // TODO add support for <select> and <textarea> elements
  // TODO add proper support for checkboxes and radio buttons
  // See: https://www.w3.org/TR/html5/forms.html#constructing-form-data-set
  val formData: HtmlExtractor[Element, Map[String, String]] =
    _.select("input")
      .filter(_.hasAttr("name"))
      .map { e => e.attr("name") -> (if (e.hasAttr("value")) e.attr("value") else "") }
      .toMap

  val formDataAndAction: HtmlExtractor[Element, (Map[String, String], String)] = { elems =>
    (formData(elems), attr("action")(elems))
  }
}
