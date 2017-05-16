package net.ruippeixotog.scalascraper.scraper

import net.ruippeixotog.scalascraper.model.{ Element, ElementQuery }

/**
  * An object containing `HtmlExtractor` instances for extracting primitive data such as text, elements or attributes,
  * as well as more complex information such as form data. Because they do perform little to no navigation through the
  * document, they are typically preceded by a CSS query defining the location in the HTML document of the data to be
  * retrieved.
  */
object ContentExtractors {
  @inline private[this] implicit def funcToExtractor[E <: Element, A](f: ElementQuery[E] => A) = HtmlExtractor(f)

  /**
    * An extractor for the first element matched.
    */
  val element: HtmlExtractor[Element, Element] = _.head

  /**
    * An extractor for an `ElementQuery` with the matched elements.
    */
  val elements: HtmlExtractor[Element, ElementQuery[Element]] = identity[ElementQuery[Element]](_)

  /**
    * An extractor for a list of the matched elements.
    */
  val elementList: HtmlExtractor[Element, List[Element]] = _.toList

  /**
    * An extractor for the first element matched. It retains the concrete type of the elements being extracted.
    */
  val pElement = new PolyHtmlExtractor {
    type Out[E] = E
    def apply[E <: Element]: HtmlExtractor[E, E] = _.head
  }

  /**
    * An extractor for an `ElementQuery` with the matched elements. It retains the concrete type of the elements being
    * extracted.
    */
  val pElements = new PolyHtmlExtractor {
    type Out[E <: Element] = ElementQuery[E]
    def apply[E <: Element]: HtmlExtractor[E, ElementQuery[E]] = identity[ElementQuery[E]](_)
  }

  /**
    * An extractor for a list of the matched elements. It retains the concrete type of the elements being extracted.
    */
  val pElementList = new PolyHtmlExtractor {
    type Out[E] = List[E]
    def apply[E <: Element]: HtmlExtractor[E, List[E]] = _.toList
  }

  /**
    * An extractor for the text in the first element matched.
    */
  val text: HtmlExtractor[Element, String] = _.head.text

  /**
    * An extractor for a lazy iterable of the text in each element matched.
    */
  val texts: HtmlExtractor[Element, Iterable[String]] = _.map(_.text)

  /**
    * An extractor for the text in all matched elements.
    */
  val allText: HtmlExtractor[Element, String] = _.map(_.text).mkString

  /**
    * An extractor for the value of an attribute of the first matched element.
    *
    * @param attr the attribute name to extract
    * @return an extractor for an attribute of the first matched element.
    */
  def attr(attr: String): HtmlExtractor[Element, String] = _.head.attr(attr)

  /**
    * An extractor for a lazy iterable of the value of an attribute of each matched element.
    *
    * @param attr the attribute name to extract
    * @return an extractor for a lazy iterable of the value of an attribute of each matched element.
    */
  def attrs(attr: String): HtmlExtractor[Element, Iterable[String]] = _.map(_.attr(attr))

  /**
    * An extractor for the form data present in the matched elements.
    */
  // TODO add support for <select> and <textarea> elements
  // TODO add proper support for checkboxes and radio buttons
  // See: https://www.w3.org/TR/html5/forms.html#constructing-form-data-set
  val formData: HtmlExtractor[Element, Map[String, String]] =
    _.select("input")
      .filter(_.hasAttr("name"))
      .map { e => e.attr("name") -> (if (e.hasAttr("value")) e.attr("value") else "") }
      .toMap

  /**
    * An extractor for the form data present in the matched elements, together with the submission URL in the form.
    */
  val formDataAndAction: HtmlExtractor[Element, (Map[String, String], String)] = { elems =>
    (formData(elems), attr("action")(elems))
  }
}
