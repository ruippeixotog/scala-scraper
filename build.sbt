import ReleaseTransformations._

ThisBuild / organization := "net.ruippeixotog"

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.7")

lazy val core = project
  .in(file("core"))
  .enablePlugins(ModuleMdocPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper",
    libraryDependencies ++= Seq(
      "com.github.nscala-time" %% "nscala-time" % "2.30.0",
      "net.sourceforge.htmlunit" % "htmlunit" % "2.54.0",
      "org.jsoup" % "jsoup" % "1.14.3",
      "org.scalaz" %% "scalaz-core" % "7.3.5",
      "com.typesafe.akka" %% "akka-http" % "10.1.13" % "test",
      "com.typesafe.akka" %% "akka-stream" % "2.6.16" % "test",
      "org.slf4j" % "slf4j-nop" % "1.7.32" % "test",
      "org.specs2" %% "specs2-core" % "4.13.0" % "test"
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
      "com.typesafe" % "config" % "1.4.1",
      "org.specs2" %% "specs2-core" % "4.13.0" % "test"
    )
  )

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"
  ),
  scalacOptions ++= baseScalacOptions ++ (CrossVersion
    .partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor <= 12 =>
      Seq("-Ypartial-unification")
    case _ => Seq.empty[String]
  }),
  scalafmtOnCompile := true,
  Test / fork := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  homepage := Some(url("https://github.com/ruippeixotog/scala-scraper")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ruippeixotog/scala-scraper"),
      "scm:git:https://github.com/ruippeixotog/scala-scraper.git",
      "scm:git:git@github.com:ruippeixotog/scala-scraper.git"
    )
  ),
  developers := List(
    Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("http://www.ruippeixotog.net"))
  )
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
