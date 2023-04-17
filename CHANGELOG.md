### 3.1.0 (Apr 17, 2023)

- New features
  - Added a new `ownText` method to `Element`.
- Breaking changes
  - Upgraded htmlunit to 3.x.

### 3.0.0 (May 25, 2022)

Support for Scala 3.1 was introduced.

- New features
  - Added a new `withProxy` method to `Browser`, allowing users to configure the proxy regardless of the browser
    implementation being used.
- Breaking changes
  - Removed all previously deprecated code.

### 2.2.1 (May 9, 2021)

Support for Scala 2.11 was dropped.

### 2.2.0 (Sep 18, 2019)

Support for Scala 2.13 was introduced.

### 2.1.0 (Jan 15, 2018)
 
- Deprecations
  - `ProxyUtils` was deprecated in favor of setting proxy servers per `Browser` instance (see below);
- New features
  - `JsoupBrowser` and `HtmlUnitBrowser` can now be created with proxy settings that are applied only to the created
    instance, superseeding the usage of `ProxyUtils`;
  - Added a new `table` context extractor allowing the extraction of cells from HTML tables.

### 2.0.0 (Jul 21, 2017)

- Breaking changes
  - Extracting using a CSS query string as extractor will now extract elements instead of text. This allows easier
    chaining of extractors and CSS selectors and fits more nicely the current extractor model. The old behavior can be
    recovered by wrapping the CSS query string in the `texts` content extractor, e.g. `doc >> texts("myQuery")`;
  - `HtmlExtractor`, `HtmlValidator` and `ElementQuery` now have an additional type parameter for the type of `Element`
    they work on. If you have custom instances of one of those classes, filling the missing parameter with `Element`
    (which is a superclass of all elements) should be enough for them to work with all source code using
    scala-scraper 1.x;
  - Methods for loading extractors and validators from a config were extracted to a separate module. In order to use
    them users must add `scala-scraper-config` to their SBT dependencies and import
    `net.ruippeixotog.scalascraper.config.dsl.DSL._`;
  - The implicit conversion of `Validated/Either` to a `RightProjection` in order to expose `foreach`, `map` and
    `flatMap` in for comprehensions was moved to a separate object that is not imported together with the DSL. Either
    upgrade to Scala 2.12 (in which `Either` is already right-biased) or import the new
    `net.ruippeixotog.scalascraper.util.EitherRightBias` support object;
- Deprecations
  - `SimpleExtractor` and `SimpleValidator` are now deprecated. The classes remain available for the time being, but DSL
    methods that returned those classes now return only `HtmlExtractor` and `HtmlValidator` instances;
  - The `Validated` type alias is now deprecated. Users should now use `Either`, `Right` and `Left` directly;
  - The `asDate` content parser was deprecated in favor of `asLocalDate` and `asDateTime`;
  - The DSL validation operator `~/~` was renamed to `>/~` in order to have the same precedence as the extraction
    operators `>>` and `>?>`;
  - The `and` DSL operator is deprecated and will be removed in future versions;
- New features
  - The concrete type of the models in scala-scraper is now passed down from the `Browser` to `Element` instances
    extracted from documents. This allows users to use features unique of each browser (such as modifying or interacting
    with elements) while still using the scala-scraper DSL to exteact and query them;
  - `HtmlExtractor[E, A]` is now a proper instance of `ElementQuery[E] => A` and have `map` and `mapQuery` methods to
    map the extraction results and the preceding query, respectively;
  - Content extractors, which were previously just functions, are now full-fledged `HtmlExtractor` instances and can be
    used by themselves, e.g. `doc >> elements`, `doc >> elementList("myQuery") >> formData`;
  - A new `PolyHtmlExtractor` class was created, allowing the implementation of extractors whose return type depends on
    the type of the element or document being extracted;
  - Overall code cleanup and simplification of some concepts.

### 1.2.1 (Apr 30, 2017)

- Bug fixes
  - Fix type parameter usage in three-arg `>?>` DSL operator.

### 1.2.0 (Dec 6, 2016)

- New features
  - Support for Scala 2.12;
  - New method `closeAll` in `HtmlUnitBrowser`, for closing opened windows;
  - New model `Node` representing a DOM node - in this library, either a `ElementNode` or a `TextNode`;
  - New methods `childNodes` and `siblingNodes` in `Element`.

### 1.1.0 (Sep 25, 2016)

- New features
  - New methods `clearCookies`, `parseInputStream` and `parseResource` in `Browser`;
  - New methods `hasAttr` and `siblings` in `Element`;
  - Support for SOCKS proxies.
- Bug fixes
  - Correct handling of missing name and value attributes in the `formData` extractor.

### 1.0.0 (Apr 5, 2016)

First stable version.
