# Config module for Scala Scraper

A Scala Scraper module allowing extractors and validators to be loaded from config files.

_NOTE: this module is in a beta stage. Please expect API changes in future releases._

## Quick Start

To use this module in an existing SBT project with Scala 2.11 or newer and `scala-scraper`, add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "net.ruippeixotog" %% "scala-scraper-config" % "3.1.3"
```

The extra DSL methods offered by this module can be imported using:

```mdoc:silent
import net.ruippeixotog.scalascraper.config.dsl.DSL._
```

Matchers and validators can be loaded from a [Typesafe config](https://github.com/typesafehub/config) using the provided methods `matcherAt`, `validatorAt` and `validatorsAt`. Take a look at the [application.conf](src/test/resources/application.conf) config used in the [unit tests](src/test/scala/net/ruippeixotog/scalascraper/config/dsl/ConfigLoadingHelpersSpec.scala) to see how they can be used.
