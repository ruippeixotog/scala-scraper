package net.ruippeixotog.scalascraper.scraper

import scala.util.Try

import net.ruippeixotog.scalascraper.model.{Element, ElementQuery}

/** An object containing `HtmlExtractor` instances for extracting primitive data such as text, elements or attributes,
  * as well as more complex information such as form data. Because they do perform little to no navigation through the
  * document, they are typically preceded by a CSS query defining the location in the HTML document of the data to be
  * retrieved.
  */
object ContentExtractors {
  @inline private[this] implicit def funcToExtractor[E <: Element, A](f: ElementQuery[E] => A): HtmlExtractor[E, A] =
    HtmlExtractor(f)

  /** An extractor for the first element matched.
    */
  val element: HtmlExtractor[Element, Element] = _.head

  /** An extractor for an `ElementQuery` with the matched elements.
    */
  val elements: HtmlExtractor[Element, ElementQuery[Element]] = identity[ElementQuery[Element]](_)

  /** An extractor for a list of the matched elements.
    */
  val elementList: HtmlExtractor[Element, List[Element]] = _.toList

  /** An extractor for the first element matched. It retains the concrete type of the elements being extracted.
    */
  val pElement = new PolyHtmlExtractor {
    type Out[E] = E
    def apply[E <: Element]: HtmlExtractor[E, E] = _.head
  }

  /** An extractor for an `ElementQuery` with the matched elements. It retains the concrete type of the elements being
    * extracted.
    */
  val pElements = new PolyHtmlExtractor {
    type Out[E <: Element] = ElementQuery[E]
    def apply[E <: Element]: HtmlExtractor[E, ElementQuery[E]] = identity[ElementQuery[E]](_)
  }

  /** An extractor for a list of the matched elements. It retains the concrete type of the elements being extracted.
    */
  val pElementList = new PolyHtmlExtractor {
    type Out[E] = List[E]
    def apply[E <: Element]: HtmlExtractor[E, List[E]] = _.toList
  }

  /** An extractor for the text in the first element matched.
    */
  val text: HtmlExtractor[Element, String] = _.head.text

  /** An extractor for a lazy iterable of the text in each element matched.
    */
  val texts: HtmlExtractor[Element, Iterable[String]] = _.map(_.text)

  /** An extractor for the text in all matched elements.
    */
  val allText: HtmlExtractor[Element, String] = _.map(_.text).mkString

  /** An extractor for the value of an attribute of the first matched element.
    *
    * @param attr
    *   the attribute name to extract
    * @return
    *   an extractor for an attribute of the first matched element.
    */
  def attr(attr: String): HtmlExtractor[Element, String] = _.head.attr(attr)

  /** An extractor for a lazy iterable of the value of an attribute of each matched element.
    *
    * @param attr
    *   the attribute name to extract
    * @return
    *   an extractor for a lazy iterable of the value of an attribute of each matched element.
    */
  def attrs(attr: String): HtmlExtractor[Element, Iterable[String]] = _.map(_.attr(attr))

  /** An extractor for the form data present in the matched elements.
    */
  // TODO add support for <select> and <textarea> elements
  // TODO add proper support for checkboxes and radio buttons
  // See: https://www.w3.org/TR/html5/forms.html#constructing-form-data-set
  val formData: HtmlExtractor[Element, Map[String, String]] =
    _.select("input")
      .filter(_.hasAttr("name"))
      .map { e => e.attr("name") -> (if (e.hasAttr("value")) e.attr("value") else "") }
      .toMap

  /** An extractor for the form data present in the matched elements, together with the submission URL in the form.
    */
  val formDataAndAction: HtmlExtractor[Element, (Map[String, String], String)] = { elems =>
    (formData(elems), attr("action")(elems))
  }

  /** An extractor for the cells of an HTML table.
    *
    * Cells spanning multiple rows or columns are repeated in each of the positions they occupy. As such, well-formed
    * rectangular tables always result in a `Vector` of `Vector`s with identical sizes.
    *
    * Rows in `thead` elements are always presented first, while rows inside `tfoot` elements are always at the end.
    */
  val table: HtmlExtractor[Element, Vector[Vector[Element]]] = { elems =>
    case class OpenCell(idx: Int, colspan: Int, remRowspan: Int, cell: Element)

    def buildRow(idx: Int, cellElems: List[Element], openCells: List[OpenCell]): (List[Element], List[OpenCell]) = {
      // This will suppress "match may not be exhaustive warning" for case input: (Nil, List(_)),
      // since it contains in case (cs, oc :: ocTail) if cs.isEmpty || idx >= oc.idx
      ((cellElems, openCells): (List[Element], List[OpenCell]) @unchecked) match {
        case (Nil, Nil) => (Nil, Nil)

        case (cs, oc :: ocTail) if cs.isEmpty || idx >= oc.idx =>
          val (tailCells, tailNewOc) = buildRow(idx + oc.colspan, cs, ocTail)
          val newOc = if (oc.remRowspan <= 1) Nil else List(oc.copy(remRowspan = oc.remRowspan))
          (List.fill(oc.colspan)(oc.cell) ::: tailCells, newOc ::: tailNewOc)

        case (c :: cTail, ocs) =>
          val colspan = Try(c.attr("colspan").toInt).getOrElse(1)
          val rowspan = Try(c.attr("rowspan").toInt).getOrElse(1)

          val (tailCells, tailNewOc) = buildRow(idx + colspan, cTail, ocs)
          val newOc = if (rowspan <= 1) Nil else List(OpenCell(idx, colspan, rowspan - 1, c))
          (List.fill(colspan)(c) ::: tailCells, newOc ::: tailNewOc)
      }
    }

    def buildTable(rows: List[Element], openCells: List[OpenCell]): List[List[Element]] =
      (rows, openCells) match {
        case (Nil, Nil) => Nil

        case (r :: rs, ocs) =>
          val (rowCells, nextOcs) = buildRow(0, r.select("th,td").toList, ocs)
          rowCells :: buildTable(rs, nextOcs)

        case (Nil, ocs) =>
          val (rowCells, nextOcs) = buildRow(0, Nil, ocs)
          rowCells :: buildTable(Nil, nextOcs)
      }

    val rows = elems.select("thead > tr") ++ elems.select("tbody > tr") ++ elems.select("tfoot > tr")
    buildTable(rows.toList, Nil).map(_.toVector).toVector
  }
}
