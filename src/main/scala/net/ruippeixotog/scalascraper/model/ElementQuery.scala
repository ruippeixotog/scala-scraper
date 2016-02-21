package net.ruippeixotog.scalascraper.model

class ElementQuery(itThunk: => Iterator[Element]) extends Iterable[Element] {
  def iterator = itThunk

  def select(query: String): ElementQuery = {
    lazy val s = iterator.flatMap(_.select(query)).toStream
    new ElementQuery(s.iterator)
  }

  def selectFirst(query: String): Option[Element] = select(query).headOption

  override def equals(obj: Any) = obj match {
    case q: ElementQuery => iterator.sameElements(q.iterator)
    case _ => false
  }

  override def hashCode() = iterator.toSeq.hashCode()
}

object ElementQuery {
  def apply(): ElementQuery = new ElementQuery(Iterator.empty)
  def apply(elem: Element): ElementQuery = new ElementQuery(Iterator(elem))
  def apply(coll: Iterable[Element]): ElementQuery = new ElementQuery(coll.iterator)
}
