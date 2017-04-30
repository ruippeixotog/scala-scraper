package net.ruippeixotog.scalascraper.dsl

import net.ruippeixotog.scalascraper.model._

/**
  * A type class indicating that an [[ElementQuery]] of some [[Element]] type can be created from an object of a given
  * type.
  *
  * @tparam A the type of the object to be made into an `ElementQuery`
  */
trait ToQuery[A] {

  /**
    * The type of the element in the `ElementQuery`.
    */
  type Out <: Element

  /**
    * Creates an `ElementQuery` for an object of type `A`.
    *
    * @param a the object for which an `ElementQuery` is to be created
    * @return an `ElementQuery` for the given object.
    */
  def apply(a: A): ElementQuery[Out]
}

object ToQuery extends LowerPriorityToQuery {
  type Aux[A, E <: Element] = ToQuery[A] { type Out = E }

  def apply[A](implicit toQuery: ToQuery[A]): Aux[A, toQuery.Out] = toQuery

  implicit def queryToQuery[E <: Element] = new ToQuery[ElementQuery[E]] {
    type Out = E
    def apply(query: ElementQuery[E]) = query
  }

  implicit def typedElemToQuery[E <: Element.Strict[E]] = new ToQuery[E] {
    type Out = E
    def apply(elem: E) = ElementQuery(elem)
  }

  implicit def typedDocToQuery[D <: Document, E <: Element.Strict[E]](implicit ev: D <:< Document.Typed[E]) = new ToQuery[D] {
    type Out = E
    def apply(doc: D) = ElementQuery(ev(doc).root)
  }
}

trait LowerPriorityToQuery {

  implicit def elemToQuery[E <: Element] = new ToQuery[E] {
    type Out = Element
    def apply(elem: E) = ElementQuery[Element](elem)
  }

  implicit def docToQuery[D <: Document] = new ToQuery[D] {
    type Out = Element
    def apply(doc: D) = ElementQuery[Element](doc.root)
  }
}
