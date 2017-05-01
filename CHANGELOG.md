### 2.0.0 (unreleased)

- Breaking changes
  - `HtmlExtractor`, `HtmlValidator` and `ElementQuery` now have an additional type parameter for the type of `Element`
    they work on. Filling it with `Element` (which is a superclass of all elements) should be enough for them to work
    with all source code using scala-scraper 1.x;
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

- New features
  - The concrete type of the models in scala-scraper is now passed down from the `Browser` to `Element` instances
    extracted from documents. This allows users to use features unique of each browser (such as modifying or interacting
    with elements) while still using the scala-scraper DSL to exteact and query them.

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
