package net.ruippeixotog.scalascraper.model

/** The result of a query to an [[Element]]. It works as a collection of `Element` instances and provides a way to
  * further query the elements.
  */
trait ElementQuery[+E <: Element] extends Iterable[E] {

  /** Executes an additional query over the elements of this query using a CSS selector.
    *
    * Semantically, the result of returned composite query is equivalent to iterating over the elements of this query,
    * applying the CSS selector on each individual node and flattening the result while eliminating duplicate results.
    *
    * @param query
    *   the CSS selector used to select elements to be returned
    * @return
    *   an `ElementQuery` instance representing the result of the composed query
    */
  def select(query: String): ElementQuery[E]
}

private[model] class RootElementQuery[E <: Element](private val target: E, exec: String => Iterator[E])
    extends ElementQuery[E] {

  def iterator = Iterator(target)

  def select(query: String): ElementQuery[E] =
    new LazyElementQuery(query.split(","), target, exec)

  override def equals(obj: Any) =
    obj match {
      case q: ElementQuery[_] => iterator.sameElements(q.iterator)
      case _ => false
    }

  override def hashCode() = iterator.toSeq.hashCode()

  override def toString() = s"RootElementQuery($target)"
}

private[model] class LazyElementQuery[E <: Element](
    private val queries: Seq[String],
    private val target: E,
    exec: String => Iterator[E]
) extends ElementQuery[E] {

  def iterator = exec(queries.mkString(","))

  def select(query: String): ElementQuery[E] = {
    val newQueries = for { q1 <- queries; q2 <- query.split(",") } yield s"$q1 $q2"
    new LazyElementQuery(newQueries, target, exec)
  }

  override def equals(obj: Any) =
    obj match {
      case q: ElementQuery[_] => iterator.sameElements(q.iterator)
      case _ => false
    }

  override def hashCode() = iterator.toSeq.hashCode()

  override def toString() = s"LazyElementQuery($queries, $target)"
}

object ElementQuery {

  def apply[E <: Element.Upper[E]](target: E): ElementQuery[E] =
    new RootElementQuery(target, target.select(_).iterator)

  def apply[E <: Element.Upper[E]](cssQuery: String, target: E): ElementQuery[E] =
    new LazyElementQuery(cssQuery.split(",").toList, target, target.select(_).iterator)

  def apply[E <: Element.Upper[E]](cssQuery: String, target: E, exec: String => Iterator[E]): ElementQuery[E] =
    new LazyElementQuery(cssQuery.split(",").toList, target, exec)
}
