# Scala Scraper [![Build Status](https://github.com/ruippeixotog/scala-scraper/workflows/CI/badge.svg?branch=master)](https://github.com/ruippeixotog/scala-scraper/actions?query=workflow%3ACI+branch%3Amaster) [![Coverage Status](https://coveralls.io/repos/github/ruippeixotog/scala-scraper/badge.svg?branch=master)](https://coveralls.io/github/ruippeixotog/scala-scraper?branch=master) [![Maven Central](https://img.shields.io/maven-central/v/net.ruippeixotog/scala-scraper_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/net.ruippeixotog/scala-scraper_2.12) [![Join the chat at https://gitter.im/ruippeixotog/scala-scraper](https://badges.gitter.im/ruippeixotog/scala-scraper.svg)](https://gitter.im/ruippeixotog/scala-scraper)

A library providing a DSL for loading and extracting content from HTML pages.

Take a look at [Examples.scala](core/src/test/scala/net/ruippeixotog/scalascraper/Examples.scala) and at the [unit specs](core/src/test/scala/net/ruippeixotog/scalascraper) for usage examples or keep reading for more thorough documentation. Feel free to use [GitHub Issues](https://github.com/ruippeixotog/scala-scraper/issues) for submitting any bug or feature request and [Gitter](https://gitter.im/ruippeixotog/scala-scraper) to ask questions.

This README contains the following sections:

- [Quick Start](#quick-start)
- [Core Model](#core-model)
- [Browsers](#browsers)
- [Content Extraction](#content-extraction)
- [Content Validation](#content-validation)
- [Other DSL Features](#other-dsl-features)
- [Using Browser-Specific Features](#using-browser-specific-features)
- [Working Behind an HTTP/HTTPS Proxy](#working-behind-an-httphttps-proxy)
- [Integration with Typesafe Config](#integration-with-typesafe-config)
- [New Features and Migration Guide](#new-features-and-migration-guide)
- [Copyright](#copyright)

## Quick Start

To use Scala Scraper in an existing SBT project with Scala 2.11 or newer, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.0"
```

If you are using an older version of this library, see this document for the version you're using: [1.x](https://github.com/ruippeixotog/scala-scraper/blob/v1.2.1/README.md), [0.1.2](https://github.com/ruippeixotog/scala-scraper/blob/v0.1.2/README.md), [0.1.1](https://github.com/ruippeixotog/scala-scraper/blob/v0.1.1/README.md), [0.1](https://github.com/ruippeixotog/scala-scraper/blob/v0.1/README.md).

An implementation of the `Browser` trait, such as `JsoupBrowser`, can be used to fetch HTML from the web or to parse a local HTML file or string:

```scala
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

val browser = JsoupBrowser()
val doc = browser.parseFile("core/src/test/resources/example.html")
val doc2 = browser.get("http://example.com")
```

The returned object is a `Document`, which already provides several methods for manipulating and querying HTML elements. For simple use cases, it can be enough. For others, this library improves the content extracting process by providing a powerful DSL.

You can open the [example.html](core/src/test/resources/example.html) file loaded above to follow the examples throughout the README.

First of all, the DSL methods and conversions must be imported:

```scala
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
```

Content can then be extracted using the `>>` extraction operator and CSS queries:

```scala
import net.ruippeixotog.scalascraper.model._

// Extract the text inside the element with id "header"
doc >> text("#header")
// res0: String = "Test page h1"

// Extract the <span> elements inside #menu
val items = doc >> elementList("#menu span")
// items: List[Element] = List(
//   JsoupElement(<span><a href="#home">Home</a></span>),
//   JsoupElement(<span><a href="#section1">Section 1</a></span>),
//   JsoupElement(<span class="active">Section 2</span>),
//   JsoupElement(<span><a href="#section3">Section 3</a></span>)
// )

// From each item, extract all the text inside their <a> elements
items.map(_ >> allText("a"))
// res1: List[String] = List("Home", "Section 1", "", "Section 3")

// From the meta element with "viewport" as its attribute name, extract the
// text in the content attribute
doc >> attr("content")("meta[name=viewport]")
// res2: String = "width=device-width, initial-scale=1"
```

If the element may or may not be in the page, the `>?>` tries to extract the content and returns it wrapped in an `Option`:

```scala
// Extract the element with id "footer" if it exists, return `None` if it
// doesn't:
doc >?> element("#footer")
// res3: Option[Element] = Some(
//   JsoupElement(
//     <div id="footer"><span>No copyright 2014</span>
// </div>
//   )
// )
```

With only these two operators, some useful things can already be achieved:

```scala
// Go to a news website and extract the hyperlink inside the h1 element if it
// exists. Follow that link and print both the article title and its short
// description (inside ".lead")
for {
  headline <- browser.get("http://observador.pt") >?> element("h1 a")
  headlineDesc = browser.get(headline.attr("href")) >> text(".lead")
} println("== " + headline.text + " ==\n" + headlineDesc)
```

In the next two sections the core classes used by this library are presented. They are followed by a description of the full capabilities of the DSL, including the ability to parse content after extracting, validating the contents of a page and defining custom extractors or validators.

## Core Model

The library represents HTML documents and their elements by [Document](core/src/main/scala/net/ruippeixotog/scalascraper/model/Document.scala) and [Element](core/src/main/scala/net/ruippeixotog/scalascraper/model/Element.scala) objects, simple interfaces containing methods for retrieving information and navigating through the DOM.

[Browser](core/src/main/scala/net/ruippeixotog/scalascraper/browser/Browser.scala) implementations are the entrypoints for obtaining `Document` instances. Most notably, they implement `get`, `post`, `parseFile` and `parseString` methods for retrieving documents from different sources. Depending on the browser used, `Document` and `Element` instances may have different semantics, mainly on their immutability guarantees.

## Browsers

The library currently provides two built-in implementations of `Browser`:

* [JsoupBrowser](core/src/main/scala/net/ruippeixotog/scalascraper/browser/JsoupBrowser.scala) is backed by [jsoup](http://jsoup.org/), a Java HTML parser library. `JsoupBrowser` provides powerful and efficient document querying, but it doesn't run JavaScript in the pages. As such, it is limited to working strictly with the HTML sent in the page source;
* [HtmlUnitBrowser](core/src/main/scala/net/ruippeixotog/scalascraper/browser/HtmlUnitBrowser.scala) is based on [HtmlUnit](http://htmlunit.sourceforge.net), a GUI-less browser for Java programs. `HtmlUnitBrowser` simulates thoroughly a web browser, executing JavaScript code in the pages in addition to parsing HTML. It supports several compatibility modes, allowing it to emulate browsers such as Internet Explorer.

Due to its speed and maturity, `JsoupBrowser` is the recommended browser to use when JavaScript execution is not needed. More information about each browser and its semantics can be obtained in the Scaladoc of each implementation.

## Content Extraction

The `>>` and `>?>` operators shown above accept an `HtmlExtractor` as their right argument, a trait with a very simple interface:

```scala
trait HtmlExtractor[-E <: Element, +A] {
  def extract(doc: ElementQuery[E]): A
}
```

One can always create a custom extractor by implementing `HtmlExtractor`. However, the DSL provides several ways to create `HtmlExtractor` instances, which should be enough in most situations. In general, you can use the `extractor` factory method:

```
doc >> extractor(<cssQuery>, <contentExtractor>, <contentParser>)
```

Where the arguments are:

* **cssQuery**: the CSS query used to select the elements to be processed;
* **contentExtractor**: the content to be extracted from the selected elements, e.g. the element objects themselves, their text, a specific attribute, form data;
* **contentParser**: an optional parser for the data extracted in the step above, such as parsing numbers and dates or using regexes.

The DSL provides several `contentExtractor` and `contentParser` instances, which were imported before with `DSL.Extract._` and `DSL.Parse._`. The full list can be seen in [ContentExtractors.scala](core/src/main/scala/net/ruippeixotog/scalascraper/scraper/ContentExtractors.scala) and [ContentParsers.scala](core/src/main/scala/net/ruippeixotog/scalascraper/scraper/ContentParsers.scala).

Some usage examples:

```scala
// Extract the date from the "#date" element
doc >> extractor("#date", text, asLocalDate("yyyy-MM-dd"))
// res5: org.joda.time.LocalDate = 2014-10-26

// Extract the text of all "#mytable td" elements and parse each of them as a number
doc >> extractor("#mytable td", texts, seq(asDouble))
// res6: TraversableOnce[Double] = non-empty iterator

// Extract an element "h1" and do no parsing (the default parsing behavior)
doc >> extractor("h1", element, asIs[Element])
// res7: Element = JsoupElement(<h1>Test page h1</h1>)
```

With the help of the implicit conversions provided by the DSL, we can write more succinctly the most common extraction cases:

* `<cssQuery>` is taken as `extractor(<cssQuery>, elements, asIs)` (by an implicit conversion);
* `<contentExtractor>` is taken as `extractor(":root", <contentExtractor>, asIs)` (content extractors are also `HtmlExtractor` instances by themselves);
* `<contentExtractor>(<cssQuery>)` is taken as `extractor(<cssQuery>, <contentExtractor>, asIs)` (by an implicit conversion).

Because of that, one can write the expressions in the Quick Start section, as well as:

```scala
// Extract all the "h3" elements (as a lazy iterable)
doc >> "h3"
// res8: ElementQuery[Element] = LazyElementQuery(
//   JsoupElement(<h3>Section 1 h3</h3>),
//   JsoupElement(<h3>Section 2 h3</h3>),
//   JsoupElement(<h3>Section 3 h3</h3>)
// )

// Extract all text inside this document
doc >> allText
// res9: String = "Test page Test page h1 Home Section 1 Section 2 Section 3 Test page h2 2014-10-26 2014-10-26T12:30:05Z 4.5 2 Section 1 h3 Some text for testing More text for testing Section 2 h3 My Form Add field Section 3 h3 3 15 15 1 No copyright 2014"

// Extract the elements with class ".active"
doc >> elementList(".active")
// res10: List[Element] = List(
//   JsoupElement(<span class="active">Section 2</span>)
// )

// Extract the text inside each "p" element
doc >> texts("p")
// res11: Iterable[String] = List(
//   "Some text for testing",
//   "More text for testing"
// )
```

## Content Validation

While scraping web pages, it is a common use case to validate if a page effectively has the expected structure. This library provides special support for creating and applying validations.

A `HtmlValidator` has the following signature:

```scala
trait HtmlValidator[-E <: Element, +R] {
  def matches(doc: ElementQuery[E]): Boolean
  def result: Option[R]
}
```

As with extractors, the DSL provides the `validator` constructor and the `>/~` operator for applying a validation to a document:

```
doc >/~ validator(<extractor>)(<matcher>)
```

Where the arguments are:

* **extractor**: an extractor as defined in the previous section;
* **matcher**: a function mapping the extracted content to a boolean indicating if the document is valid.

The result of a validation is an `Either[R, A]` instance, where `A` is the type of the document and `R` is the result type of the validation (which will be explained later).

Some validation examples:

```scala
// Check if the title of the page is "Test page"
doc >/~ validator(text("title"))(_ == "Test page")
// res12: Either[Unit, browser.DocumentType] = Right(
//   JsoupDocument(
//     <!doctype html>
// <html lang="en">
//  <head>
//   <meta charset="utf-8">
//   <meta name="viewport" content="width=device-width, initial-scale=1">
//   <title>Test page</title>
//  </head>
//  <body>
//   <div id="wrapper">
//    <div id="header">
//     <h1>Test page h1</h1>
//    </div>
//    <div id="menu"><span><a href="#home">Home</a></span> <span><a href="#section1">Section 1</a></span> <span class="active">Section 2</span> <span><a href="#section3">Section 3</a></span>
//    </div>
//    <div id="content">
//     <h2>Test page h2</h2><span id="date">2014-10-26</span> <span id="datefull">2014-10-26T12:30:05Z</span> <span id="rating">4.5</span> <span id="pages">2</span>
//     <section>
//      <h3>Section 1 h3</h3>
//      <p>Some text for testing</p>
//      <p>More text for testing</p>
//     </section>
//     <section>
//      <h3>Section 2 h3</h3><span>My Form</span>
//      <form id="myform" action="submit.html"><input type="text" name="name" value="John"> <input type="text" name="address"> <input type="submit" value="Submit"> <span><a href="#">Add field</a></span>
//      </form>
//     </section>
//     <section>
//      <h3>Section 3 h3</h3>
//      <table id="mytable">
//       <tbody>
//        <tr>
//         <td>3</td>
//         <td>15</td>
//         <td>15</td>
//         <td>1</td>
//        </tr>
//       </tbody>
//      </table>
//     </section>
//    </div>
//    <div id="footer"><span>No copyright 2014</span>
// ...

// Check if there are at least 3 ".active" elements
doc >/~ validator(".active")(_.size >= 3)
// res13: Either[Unit, browser.DocumentType] = Left(())

// Check if the text in ".desc" contains the word "blue"
doc >/~ validator(allText("#mytable"))(_.contains("blue"))
// res14: Either[Unit, browser.DocumentType] = Left(())
```

When a document fails a validation, it may be useful to identify the problem by pattern-matching it against common scraping pitfalls, such as a login page that appears unexpectedly because of an expired cookie, dynamic content that disappeared or server-side errors. If we define validators for both the success case and error cases:

```scala
val succ = validator(text("title"))(_ == "My Page")

val errors = Seq(
  validator(allText(".msg"), "Not logged in")(_.contains("sign in")),
  validator(".item", "Too few items")(_.size < 3),
  validator(text("h1"), "Internal Server Error")(_.contains("500")))
```

They can be used in combination to create more informative validations:

```scala
doc >/~ (succ, errors)
// res15: Either[String, browser.DocumentType] = Left("Too few items")
```

Validators matching errors were constructed above using an additional `result` parameter after the extractor. That value is returned wrapped in a `Left` if that particular error occurs during a validation.

## Other DSL Features

As shown before in the Quick Start section, one can try if an extractor works in a page and obtain the extracted content wrapped in an `Option`:

```scala
// Try to extract an element with id "optional", return `None` if none exist
doc >?> element("#optional")
// res16: Option[Element] = None
```

Note that when using `>?>` with content extractors that return sequences, such as `texts` and `elements`, `None` will never be returned (`Some(Seq())` will be returned instead).

If you want to use multiple extractors in a single document or element, you can pass tuples or triples to `>>`:

```scala
// Extract the text of the title element and all inputs of #myform
doc >> (text("title"), elementList("#myform input"))
// res17: (String, List[Element]) = (
//   "Test page",
//   List(
//     JsoupElement(<input type="text" name="name" value="John">),
//     JsoupElement(<input type="text" name="address">),
//     JsoupElement(<input type="submit" value="Submit">)
//   )
// )
```

The extraction operators work on `List`, `Option`, `Either` and other instances for which a [Scalaz](https://github.com/scalaz/scalaz) `Functor` instance exists. The extraction occurs by mapping over the functors:

```scala
// Extract the titles of all documents in the list
List(doc, doc) >> text("title")
// res18: List[String] = List("Test page", "Test page")

// Extract the title if the document is a `Some`
Option(doc) >> text("title")
// res19: Option[String] = Some("Test page")
```

You can apply other extractors and validators to the result of an extraction, which is particularly powerful combined with the feature shown above:

```scala
// From the "#menu" element, extract the text in the ".active" element inside
doc >> element("#menu") >> text(".active")
// res20: String = "Section 2"

// Same as above, but in a scenario where "#menu" can be absent
doc >?> element("#menu") >> text(".active")
// res21: Option[String] = Some("Section 2")

// Same as above, but check if the "#menu" has any "span" element before
// extracting the text
doc >?> element("#menu") >/~ validator("span")(_.nonEmpty) >> text(".active")
// res22: Option[Either[Unit, String]] = Some(Right("Section 2"))

// Extract the links inside all the "#menu > span" elements
doc >> elementList("#menu > span") >?> attr("href")("a")
// res23: List[Option[String]] = List(
//   Some("#home"),
//   Some("#section1"),
//   None,
//   Some("#section3")
// )
```

This library also provides a `Functor` for `HtmlExtractor`, making it possible to map over extractors and create chained extractors that can be passed around and stored like objects. For example, new extractors can be defined like this:

```scala
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

// An extractor for a list with the first link found in each "span" element
val spanLinks: HtmlExtractor[Element, List[Option[String]]] =
  elementList("span") >?> attr("href")("a")

// An extractor for the number of "span" elements that actually have links
val spanLinksCount: HtmlExtractor[Element, Int] =
  spanLinks.map(_.flatten.length)
```

You can also "prepend" a query to any existing extractor by using its `mapQuery` method:

```scala
// An extractor for `spanLinks` that are inside "#menu"
val menuLinks: HtmlExtractor[Element, List[Option[String]]] =
  spanLinks.mapQuery("#menu")
```

And they can be used just as extractors created using other means provided by the DSL:

```scala
doc >> spanLinks
// res24: List[Option[String]] = List(
//   Some("#home"),
//   Some("#section1"),
//   None,
//   Some("#section3"),
//   None,
//   None,
//   None,
//   None,
//   None,
//   Some("#"),
//   None
// )

doc >> spanLinksCount
// res25: Int = 4

doc >> menuLinks
// res26: List[Option[String]] = List(
//   Some("#home"),
//   Some("#section1"),
//   None,
//   Some("#section3")
// )
```

Just remember that you can only apply extraction operators `>>` and `>?>` to documents, elements or functors "containing" them, which means that the following is a compile-time error:

```scala
// The `texts` extractor extracts a list of strings and extractors cannot be
// applied to strings
doc >> texts("#menu > span") >> "a"
// error: value >> is not a member of Iterable[String]
// doc >> texts("#menu > span") >> "a"
// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

Finally, if you prefer not using operators for the sake of code legibility, you can use alternative methods:

```scala
// `extract` is the same as `>>`
doc extract text("title")
// res28: String = "Test page"

// `tryExtract` is the same as `>?>`
doc tryExtract element("#optional")
// res29: Option[Element] = None

// `validateWith` is the same as `>/~`
doc validateWith (succ, errors)
// res30: Either[String, browser.DocumentType] = Left("Too few items")
```

## Using Browser-Specific Features

_NOTE: this feature is in a beta stage. Please expect API changes in future releases._

At this moment, Scala Scraper is focused on providing a DSL for querying documents efficiently and elegantly. Therefore, it doesn't support directly modifying the DOM or executing actions such as clicking an element. However, since version 2.0.0 a new typed element API allows users to interact directly with the data structures of the underlying `Browser` implementation.

First of all, make sure your `Browser` instance has a concrete type, like `HtmlUnitBrowser`:

```scala
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser._

// the `typed` method on the companion object of a `Browser` returns instances
// with their concrete type
val typedBrowser: HtmlUnitBrowser = HtmlUnitBrowser.typed()

val typedDoc: HtmlUnitDocument = typedBrowser.parseFile("core/src/test/resources/example.html")
```

Note that the `val` declarations are explicitly typed for explanation purposes only; the methods work just as well when types are inferred.

The content extractors `pElement`, `pElements` and `pElementList` are special types of extractors - they are polymorphic extractors. They work just like their non-polymorphic `element`, `elements` and `elementList` extractors, but they propagate the concrete types of the elements if the document or element being extracted also has a concrete type. For example:

```scala
// extract the "a" inside the second child of "#menu"
val aElem = typedDoc >> pElement("#menu span:nth-child(2) a")
// aElem: HtmlUnitElement = HtmlUnitElement(HtmlAnchor[<a href="#section1_2">])
```

Note that extracting using CSS queries also keeps the concrete types of the elements:

```scala
// same thing as above
typedDoc >> "#menu" >> "span:nth-child(2)" >> "a" >> pElement
// res31: pElement.Out[HtmlUnitElement] = HtmlUnitElement(
//   HtmlAnchor[<a href="#section1_2">]
// )
```

Concrete element types, like `HtmlUnitElement`, expose a public `underlying` field with the underlying element object used by the browser backend. In the case of HtmlUnit, that would be a [`DomElement`](http://htmlunit.sourceforge.net/apidocs/com/gargoylesoftware/htmlunit/html/DomElement.html), which exposes a whole new range of operations:

```scala
// extract the current "href" this "a" element points to
aElem >> attr("href")
// res32: String = "#section1"

// use `underlying` to update the "href" attribute
aElem.underlying.setAttribute("href", "#section1_2")

// verify that "href" was updated
aElem >> attr("href")
// res34: String = "#section1_2"

// get the location of the document (without the host and the full path parts)
typedDoc.location.split("/").last
// res35: String = "example.html"

def click(elem: HtmlUnitElement): Unit = {
  // the type param may be needed, as the original API uses Java wildcards
  aElem.underlying.click[org.htmlunit.Page]()
}

// simulate a click on our recently modified element
click(aElem)

// check the new location
typedDoc.location.split("/").last
// res37: String = "example.html#section1_2"
```

Using the typed element API provides much more flexibility when more than querying elements is required. However, one should avoid using it unless strictly necessary, as:

* It binds code to specific `Browser` implementations, making it more difficult to change implementations later;
* The code becomes subject to changes in the API of the underlying library;
* It's heavier on the Scala type system and it is not as mature, leading to possible unexpected compilation errors. If that happens, please file an issue!

## Working Behind an HTTP/HTTPS Proxy

If you are behind an HTTP or SOCKS proxy, you can configure `Browser` implementations to make connections through it by either using the browser's appropriate constructor (implementation-dependent) or by calling `withProxy` on any browser instance:

```scala
import net.ruippeixotog.scalascraper.browser.Proxy

val browser2 = JsoupBrowser().withProxy(Proxy("example.com", 7000, Proxy.SOCKS))
```

## Integration with Typesafe Config

The [Scala Scraper Config module](modules/config/README.md) can be used to load extractors and validators from config files.

## New Features and Migration Guide

The [CHANGELOG](CHANGELOG.md) is kept updated with the bug fixes and new features of each version. When there are breaking changes, they are listed there together with suggestions for migrating old code.

## Copyright

Copyright (c) 2014-2022 Rui Gonçalves. See LICENSE for details.
