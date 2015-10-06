# Scala Scraper

A library providing a DSL for loading and extracting content from HTML pages.

Take a look at [Examples.scala](https://github.com/ruippeixotog/scala-scraper/blob/master/src/test/scala/net/ruippeixotog/scalascraper/Examples.scala) and at the [unit specs](https://github.com/ruippeixotog/scala-scraper/tree/master/src/test/scala/net/ruippeixotog/scalascraper) for usage examples or keep reading for more thorough documentation.

## Quick Start

To use Scala Scraper in an existing SBT project with Scala 2.11.x, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "0.1.1"
```

A `Browser` object can be used to fetch HTML from the web or to parse a local HTML file or a string.

```scala
import net.ruippeixotog.scalascraper.browser.Browser

val browser = new Browser
val doc = browser.get("http://example.com/")
val doc2 = browser.parseFile("file.html")
```

The returned object is a [jsoup](http://jsoup.org/) [Document](http://jsoup.org/apidocs/org/jsoup/nodes/Document.html) that already provides several methods for manipulating and querying HTML elements. For simple use cases, it can be enough. For others, this library improves the content extracting process by providing a powerful DSL.

First of all, the DSL methods and conversions must be imported:

```scala
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
```

Content can then be extracted using the `>>` extraction operator and CSS queries:

```scala
// Extract the text inside the first h1 element
val title: String = doc >> text("h1")

// Extract the elements with class "item"
val items: Seq[Element] = doc >> elements(".item")

// From each item, extract the text of the h3 element inside
val itemTitles: Seq[String] = items.map(_ >> text("h3"))

// From the meta element with "viewport" as its attribute name, extract the
// text in the content attribute
val viewport: String = doc >> attr("content")("meta[name=viewport]")
```

If the element may or may not be in the page, the `>?>` tries to extract the content and returns it wrapped in an `Option`:

```scala
// Extract the element with id "optional" if it exists, return `None` if it
// doesn't:
val title: Option[Element] = doc >?> element("#optional")
```

With only these two operators, some useful things can already be achieved:

```scala
// Go to a news website and extract the hyperlink inside the h1 element if it
// exists. Follow that link and print both the article title and its short
// description (inside ".lead")
for {
  headline <- browser.get("http://observador.pt") >?> element("h1 a")
  headlineDesc = browser.get(headline.attr("href")) >> text(".lead")
} println("==" + headline.text + "==\n" + headlineDesc)
```

The next sections describe the full capabilities of the DSL, including the ability to parse content after extracting, validating the contents of a page and defining custom extractors or validators.

## Content Extraction

The `>>` and `>?>` operators shown above accept an `HtmlExtractor` as their right argument, which has a very simple interface:

```scala
trait HtmlExtractor[+A] {
  def extract(doc: Elements): A
}
```

One can always create a custom extractor by implementing `HtmlExtractor`. However, the DSL provides several useful constructors for `HtmlExtractor` instances. In general, you can use the `extractor` factory method:

```scala
doc >> extractor(<cssQuery>, <contentExtractor>, <contentParser>)
```

Where the arguments are:

* **cssQuery**: the CSS query used to select the elements to be processed;
* **contentExtractor**: defines the content to be extracted from the selected elements, e.g. the element objects themselves, their text, a specific attribute, form data;
* **contentParser**: an optional parser for the data extracted in the step above, such as parsing numbers and dates or using regexes.

The DSL provides several `contentExtractor` and `contentParser` instances, which were imported before with `DSL.Extract._` and `DSL.Parse._`. The full list can be seen in the `ContentExtractors` and `ContentParsers` objects inside [HtmlExtractor.scala](https://github.com/ruippeixotog/scala-scraper/blob/master/src/main/scala/net/ruippeixotog/scalascraper/scraper/HtmlExtractor.scala).

Some usage examples:

```scala
// Extract the text of all ".price" elements and parse them as numbers
doc >> extractor("section .price", texts, asDouble)

// Extract the date from the "#date-taken" element
doc >> extractor("#date-taken", text, asDate("yyyy-MM-dd"))

// Extract an element "#card" and do no parsing (the default parsing behavior)
doc >> extractor("#card", element, asIs)
```

The DSL also provides implicit conversions to write more succinctly the most common extractor types:

* Writing `<contentExtractor>(<cssQuery>)` is taken as `extractor(<cssQuery>, <contentExtractor>, asIs)`;
* Writing `<cssQuery>` is taken as `extractor(<cssQuery>, texts, asIs)`.

That way, one can write the expressions in the Quick Start section, as well as:

```scala
// Extract the elements with class "article"
doc >> elements(".article")

// Extract the text inside each p element
doc >> texts("p")

// Exactly the same as the extractor above
doc >> "p"
```

## Content Validation

While scraping web pages, it is a common use case to validate if a page effectively has the expected structure. This library provides special support for creating and applying validations.

A `HtmlValidator` has the following signature:

```scala
trait HtmlValidator[+R] {
  def matches(doc: Elements): Boolean
  def result: Option[R]
}
```

As with extractors, the DSL provides the `validator` constructor and the `~/~` operator for applying a validation to a document:

```scala
doc ~/~ validator(<extractor>)(<matcher>)
```

Where the arguments are:

* **extractor**: an extractor as defined in the previous section;
* **matcher**: a function mapping the extracted content to a boolean indicating if the document is valid.

The result of a validation is a `Validated[A, R]` instance, where `A` is the type of the document and `R` is the result type of the validation (which will be explained later). A `Validated` can be either a `VSuccess(a: A)` or a `VFailure(res: R)`.

Some validation examples:

```scala
// Check if the title of the page is "My Page"
doc ~/~ validator(text("title"))(_ == "My Page") match {
  case VSuccess(_) => println("Correct!")
  case VFailure(_) => println("Wrong title")
}

// Check if there are at least 3 items
doc ~/~ validator(".item")(_.length >= 3)

// Check if the text in ".desc" contains the word "blue"
doc ~/~ validator(text(".desc"))(_.contains("blue"))
```

When a document fails a validation, it may be useful to identify the problem by pattern-matching it against common scraping pitfalls, such as a login page that appears unexpectedly because of an expired cookie, dynamic content that disappeared or server-side errors. Validators can be also used to match "error" pages instead of expected pages:

```scala
val succ = validator(text("title"))(_ == "My Page")
val errors = Seq(
  validator(text(".msg"))(_.contains("sign in")) withResult "Not logged in",
  validator(".item")(_.length < 3) withResult "Too few items",
  validator(text("h1"))(_.contains("500")) withResult "Internal Server Error")

doc ~/~ (succ, errors) match {
  case VSuccess(_) => println("yey")
  case VFailure(msg) => println(s"Error: $msg")
}
```

For validators matching errors, `withResult` should be used most times. It returns a new validator holding a `result` value which will be returned wrapped in a `VFailure` if that particular error ever occurs.

## Other DSL Features

As shown before in the Quick Start section, one can try if an extractor works in a page and obtain the extracted content wrapped in an `Option`:

```scala
// Try to extract an element with id "title", return `None` if none exist
doc >?> element("#title")
```

Note that when using `>?>` with content extractors that return sequences, such as `texts` and `elements`, `None` will never be returned (`Some(Seq())` will be returned instead).

If you want to use multiple extractors in a single document or element, you can pass tuples or triples to `>>`:

```scala
// Extract the text of the title element and all the form elements
doc >> (text("title"), elements("form"))
```

The extraction operators work on `List`, `Option`, `Either`, `Validated` and other instances for which a [Scalaz](https://github.com/scalaz/scalaz) `Functor` instance is provided. The extraction occurs by mapping over the functors:

```scala
// Extract the titles of all documents in the list
List(doc1, doc2) >> text("title")

// Extract the title if the document is a `Some`
Option(doc) >> text("title")
```

You can apply other extractors and validators to the result of an extraction, which is particularly powerful combined with the last feature shown above:

```scala
// From the "#menu" element, extract the text in the ".curr" element inside
doc >> element("#menu") >> text(".curr")

// Same as above, but in a scenario where "#menu" can be absent
doc >?> element("#menu") >> text(".curr")

// Same as above, but check if the "#menu" has any section before extracting
// the text
doc >?> element("#menu") ~/~ validator("section")(_.nonEmpty) >> text(".curr")

// Extract the links inside all the ".article" elements
doc >> elementList(".article") >> attr("href")("a")
```

This library also provides a `Functor` for `HtmlExtractor`, which makes it possible to map over extractors and create chained extractors that can be passed around and stored like objects:

```scala
// An extractor for the links inside all the ".article" elements
val linksExtractor = elementList(".article") >> attr("href")("a")
doc >> linksExtractor

// An extractor for the number of links
val linkCountExtractor = linksExtractor.map(_.length)
doc >> linkCountExtractor
```

Just remember that you can only apply extraction operators `>>` and `>?>` to jsoup documents/elements or to functors "containing" them, which means that the following is a compile-time error:

```scala
// The `texts` extractor extracts a list of strings and extractors cannot be
// applied to strings
doc >> texts(".article") >> attr("href")("a")
```

Finally, if you prefer not using operators for the sake of code legibility, you can use full method names:

```scala
// `extract` is the same as `>>`
doc extract text("title")

// `tryExtract` is the same as `>?>`
doc tryExtract element("#optional")
```

## Integration with Typesafe Config Files

Matchers and validators can be loaded from a [Typesafe config](https://github.com/typesafehub/config) using the methods `matcherAt`, `validatorAt` and `validatorsAt` of the DSL. More documentation will be available soon - meanwhile, take a look at the [examples.conf](https://github.com/ruippeixotog/scala-scraper/blob/master/src/test/resources/examples.conf) config used [in the examples](https://github.com/ruippeixotog/scala-scraper/blob/master/src/test/scala/net/ruippeixotog/scalascraper/Examples.scala) and at the [application.conf](https://github.com/ruippeixotog/scala-scraper/blob/master/src/test/resources/examples.conf) used in tests.

## Working under a HTTP/S Proxy
The only way to configure a proxy is to do it JVM-wide because JSoup does not provide a way to do it. This means that every operation using Java's HttpURLConnection will be affected by the proxy configuration.

Noticed about this you can use the ProxyUtils provided to set HTTP and HTTPS Proxy before invoke any operation on the Browser class:
```scala
ProxyUtils.setProxy("localhost", 3128)
val browser = Browser()
// Scraping operations...
```

## Copyright

Copyright (c) 2014 Rui Gonçalves. See LICENSE for details.
