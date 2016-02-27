package net.ruippeixotog.scalascraper.model

trait ElementQuery extends Iterable[Element] {
  def select(query: String): ElementQuery
}

private[model] class RootElementQuery[A](
    private val target: A,
    exec: String => Iterator[Element]) extends ElementQuery {

  def iterator = exec(":root")

  def select(query: String): ElementQuery =
    new LazyElementQuery(query.split(","), target, exec)

  override def equals(obj: Any) = obj match {
    case q: ElementQuery => iterator.sameElements(q.iterator)
    case _ => false
  }

  override def hashCode() = iterator.toSeq.hashCode()
}

private[model] class LazyElementQuery[A](
    private val queries: Seq[String],
    private val target: A,
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
}

object ElementQuery {

  def apply(target: Element): ElementQuery =
    new RootElementQuery[Element](target, target.select(_).iterator)

  def apply(cssQuery: String, target: Element): ElementQuery =
    new LazyElementQuery[Element](cssQuery.split(",").toList, target, target.select(_).iterator)

  def apply[A](target: A, exec: String => Iterator[Element]): ElementQuery =
    new RootElementQuery(target, exec)

  def apply[A](cssQuery: String, target: A, exec: String => Iterator[Element]): ElementQuery =
    new LazyElementQuery(cssQuery.split(",").toList, target, exec)
}
