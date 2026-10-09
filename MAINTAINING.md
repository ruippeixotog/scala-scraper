# Maintaining scala-scraper

This document outlines useful information for scala-scraper maintainers to do day-to-day tasks like releasing new versions.

## Releasing a new version

There are a few steps we follow every time we publish a new scala-scraper version to Sonatype:

1. Update CHANGELOG with an entry for the new version
    - You can explore all the commits since the last version with a link [like this](https://github.com/ruippeixotog/scala-scraper/compare/v3.2.0...master).
    - Consider what the next version will be. If the new release contains any breaking changes, please bump the minor version (we usually consider a breaking change to be one that is [source incompatible](https://en.wikipedia.org/wiki/Source-code_compatibility)).
2. If needed, update the version in `version.sbt`
    - At this point, the version should still be a snapshot of the version to be released. If you are releasing version `x.y.z`, the version in `version.sbt` should be `x.y.z-SNAPSHOT`.
3. Run `sbt mdoc`
    - `mdoc` uses the latest CHANGELOG entry to determine the latest stable version and exposes it as a macro. You should expect changes in generated READMEs.
4. Commit changes locally (without pushing them)
5. Run `sbt release`
    - This step requires you to have a Sonatype account correctly configured (see instructions [here](https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html#step+3%3A+Credentials)) and your account should have access to the net.ruippeixotog project there.
    - sbt will ask a few questions regarding version numbers. The defaults should all be correct, but please double-check everything.
    - When asked if you want to push changes to the remote repository, please do so.
    - Be patient - this process will take several minutes!
