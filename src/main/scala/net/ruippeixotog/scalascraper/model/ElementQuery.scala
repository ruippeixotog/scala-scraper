package net.ruippeixotog.scalascraper.model

/**
  * The result of a query to an [[Element]]. It works as a collection of `Element` instances and provides a way to
  * further query the elements.
  */
trait ElementQuery extends Iterable[Element] {

  /**
    * Executes an additional query over the elements of this query using a CSS selector.
    *
    * Semantically, the result of returned composite query is equivalent to iterating over the elements of this query,
    * applying the CSS selector on each individual node and flattening the result while eliminating duplicate results.
    *
    * @param query the CSS selector used to select elements to be returned
    * @return an `ElementQuery` instance representing the result of the composed query
    */
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

  def apply(cssQuery: String, target: Element, exec: String => Iterator[Element]): ElementQuery =
    new LazyElementQuery(cssQuery.split(",").toList, target, exec)
}
