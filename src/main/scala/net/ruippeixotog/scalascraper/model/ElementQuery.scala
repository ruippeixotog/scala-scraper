package net.ruippeixotog.scalascraper.model

trait ElementQuery extends Iterable[Element] {
  def select(query: String): ElementQuery
}

private[model] class RootElementQuery(
    private val target: Element,
    exec: String => Iterator[Element]) extends ElementQuery {

  def iterator = Iterator(target)

  def select(query: String): ElementQuery =
    new LazyElementQuery(query.split(","), target, exec)

  override def equals(obj: Any) = obj match {
    case q: ElementQuery => iterator.sameElements(q.iterator)
    case _ => false
  }

  override def hashCode() = iterator.toSeq.hashCode()

  override def toString() = s"RootElementQuery($target)"
}

private[model] class LazyElementQuery(
    private val queries: Seq[String],
    private val target: Element,
    exec: String => Iterator[Element]) extends ElementQuery {

  def iterator = exec(queries.mkString(","))

  def select(query: String): ElementQuery = {
    val newQueries = for { q1 <- queries; q2 <- query.split(",") } yield s"$q1 $q2"
    new LazyElementQuery(newQueries, target, exec)
  }

  override def equals(obj: Any) = obj match {
    case q: ElementQuery => iterator.sameElements(q.iterator)
    case _ => false
  }

  override def hashCode() = iterator.toSeq.hashCode()

  override def toString() = s"LazyElementQuery($queries, $target)"
}

object ElementQuery {

  def apply(target: Element): ElementQuery =
    new RootElementQuery(target, target.select(_).iterator)

  def apply(cssQuery: String, target: Element): ElementQuery =
    new LazyElementQuery(cssQuery.split(",").toList, target, target.select(_).iterator)

  def apply[A](cssQuery: String, target: Element, exec: String => Iterator[Element]): ElementQuery =
    new LazyElementQuery(cssQuery.split(",").toList, target, exec)
}
