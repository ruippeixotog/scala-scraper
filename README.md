# Scala Scraper

A library providing a DSL for loading and extracting content from HTML pages.

Take a look at [Examples.scala](https://github.com/ruippeixotog/scala-scraper/blob/master/src/test/scala/net/ruippeixotog/scalascraper/Examples.scala) and at the [unit specs](https://github.com/ruippeixotog/scala-scraper/tree/master/src/test/scala/net/ruippeixotog/scalascraper) for usage examples or keep reading for more thorough documentation.

## Quick Start

To use Scala Scraper in an existing SBT project with Scala 2.11.x, add the following repository and dependency to your `build.sbt`:

```scala
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "0.1-SNAPSHOT"
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

If the element may or may not be in the page, the `>?>` tries to extract the content and returns an `Option`:

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
* **contentExtractor**: defines the content to be extracted from the selected elements e.g. the element objects themselves, their text, a specific attribute, form data;
* **contentParser**: an optional parser for the data extracted in the step above, such as parsing numbers, dates or regexes from text.

The DSL provides several `contentExtractor` and `contentParser` instances, which were imported before with `DSL.Extract._` and `DSL.Parse._`. The full list can be seen in the `ContentExtractors` and `ContentParsers` objects inside [HtmlExtractor.scala](https://github.com/ruippeixotog/scala-scraper/blob/master/src/main/scala/net/ruippeixotog/scalascraper/scraper/HtmlExtractor.scala).

Some usage examples:

```scala
// Extract the text of all ".price" elements and parse them as numbers
doc >> extractor("section .price", texts, asDouble)

// Extract the date from the "#date-taken" element
doc >> extractor("#date-taken", text, asDate("yyyy-MM-dd"))

// Extract an element "#card" and do no parsing (the default behavior)
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

TODO

## Other DSL Features

As shown before in the Quick Start section, one can try if an extractor works in a page and return an `Option` as the extracted content:

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

The extraction operators work on `List`, `Option`, `Either` and other instances for which [Scalaz](https://github.com/scalaz/scalaz) provides a `Functor` instance:

```scala
// Extract the titles of all documents in the list
List(doc1, doc2) >> text("title")

// Extract the title if the document is a `Some`
Option(doc) >> text("title")
```

You can apply other extractors to the result of an extraction, which is particularly powerful combined with the last feature shown above:

```scala
// From the "#menu" element, extract the text in the ".active" element inside
doc >> element("#menu") >> text(".active")

// Same as above, but in a scenario where "#menu" can be absent
doc >?> element("#menu") >> text(".active")

// Extract the links inside all the ".article" elements
doc >> elementList(".article") >> attr("href")("a")
```

Finally, if you prefer not using operators for the sake of code legibility, you can use full method names:

```scala
// `extract` is the same as `>>`
doc extract text("title")

// `tryExtract` is the same as `>?>`
doc tryExtract element("#optional")
```

## Integration with Typesafe Config Files

TODO

## Copyright

Copyright (c) 2014 Rui Gonçalves. See LICENSE for details.
