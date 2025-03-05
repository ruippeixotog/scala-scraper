import ReleaseTransformations._

ThisBuild / organization := "net.ruippeixotog"

ThisBuild / scalaVersion := "2.12.20"
ThisBuild / crossScalaVersions := Seq("2.12.20", "2.13.16", "3.4.3")

// taken from https://github.com/scala/bug/issues/12632
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

lazy val core = project
  .in(file("core"))
  .enablePlugins(ModuleMdocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper",
    libraryDependencies ++= Seq(
      "com.github.nscala-time" %% "nscala-time" % "3.0.0",
      "org.htmlunit" % "htmlunit" % "4.10.0",
      "org.jsoup" % "jsoup" % "1.19.1",
      "org.scalaz" %% "scalaz-core" % "7.3.8",
      "com.typesafe.akka" %% "akka-http" % "10.2.10" % "test" cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-stream" % "2.6.20" % "test" cross CrossVersion.for3Use2_13,
      "org.slf4j" % "slf4j-nop" % "2.0.17" % "test",
      "org.specs2" %% "specs2-core" % "4.20.9" % "test"
    ),
    mdocOut := file(".")
  )

val baseScalacOptions = Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds"
)

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(core)
  .enablePlugins(ModuleMdocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper-config",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.3",
      "org.specs2" %% "specs2-core" % "4.20.9" % "test"
    )
  )

lazy val commonSettings = Seq(
  // format: off
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",

  scalacOptions ++= baseScalacOptions ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor <= 12 =>
        List("-Ypartial-unification")
      case _ =>
        Nil
    }),

  scalafmtOnCompile := true,
  Test / fork := true,

  homepage := Some(url("https://github.com/ruippeixotog/scala-scraper")),
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ruippeixotog/scala-scraper"),
      "scm:git:https://github.com/ruippeixotog/scala-scraper.git",
      "scm:git:git@github.com:ruippeixotog/scala-scraper.git"
    )
  ),
  developers := List(
    Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("http://www.ruippeixotog.net"))
  ),

  publishMavenStyle := true,
  Test / publishArtifact := false,
  publishTo := sonatypePublishToBundle.value,
  // format: on
)

// do not publish the root project
publish / skip := true

releaseCrossBuild := true
releaseTagComment := s"Release ${(ThisBuild / version).value}"
releaseCommitMessage := s"Set version to ${(ThisBuild / version).value}"

// necessary due to https://github.com/sbt/sbt-release/issues/184
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
