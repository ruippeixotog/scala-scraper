package net.ruippeixotog.scalascraper.model

/**
  * A representation of a HTML DOM node. Only two types of nodes are supported: element nodes (`ElementNode`) containing
  * `Element` instances and text nodes (`TextNode`) containing plain text.
  *
  * Most methods in scala-scraper deal with and return `Element` instances directly, instead of nodes. `Node` lists can
  * be retrieved by using the `childNodes` and `siblingNodes` methods of an `Element`.
  */
sealed trait Node

/**
  * A `Node` representing a DOM element.
  * @param element the DOM element
  */
case class ElementNode(element: Element) extends Node

/**
  * A `Node` representing a DOM text node.
  * @param content the text content of the node
  */
case class TextNode(content: String) extends Node
