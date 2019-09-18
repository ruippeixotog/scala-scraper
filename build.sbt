import ReleaseTransformations._
import scalariform.formatter.preferences._

organization in ThisBuild := "net.ruippeixotog"

scalaVersion in ThisBuild := "2.12.9"
crossScalaVersions in ThisBuild := Seq("2.11.12", "2.12.9", "2.13.0")

lazy val core = project.in(file("core"))
  .enablePlugins(TutPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper",

    libraryDependencies ++= Seq(
      "com.github.nscala-time"     %% "nscala-time"          % "2.22.0",
      "net.sourceforge.htmlunit"    % "htmlunit"             % "2.34.1",
      "org.jsoup"                   % "jsoup"                % "1.11.3",
      "org.scalaz"                 %% "scalaz-core"          % "7.2.28",
      "com.typesafe.akka"          %% "akka-http"            % "10.1.9"               % "test",
      "com.typesafe.akka"          %% "akka-stream"          % "2.5.25"               % "test",
      "org.slf4j"                   % "slf4j-nop"            % "1.7.26"               % "test",
      "org.specs2"                 %% "specs2-core"          % "4.5.1"                % "test"),

    tutTargetDirectory := file("."))

val baseScalacOptions = Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds"
)

lazy val config = project.in(file("modules/config"))
  .dependsOn(core)
  .enablePlugins(TutPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper-config",

    libraryDependencies ++= Seq(
      "com.typesafe"                % "config"               % "1.3.3",
      "org.specs2"                 %% "specs2-core"          % "4.5.1"                % "test"))

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"),

  scalariformPreferences := scalariformPreferences.value
    .setPreference(DanglingCloseParenthesis, Prevent)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true),

  scalacOptions ++= baseScalacOptions ++ (CrossVersion
    .partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor <= 12 =>
          Seq("-Ypartial-unification")
        case _ => Seq.empty[String]
      }),

  fork in Test := true,

  tutTargetDirectory := baseDirectory.value,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  homepage := Some(url("https://github.com/ruippeixotog/scala-scraper")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/ruippeixotog/scala-scraper"),
    "scm:git:https://github.com/ruippeixotog/scala-scraper.git",
    "scm:git:git@github.com:ruippeixotog/scala-scraper.git")),
  developers := List(
    Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("http://www.ruippeixotog.net"))))

// do not publish the root project
skip in publish := true

releaseCrossBuild := true
releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

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
  setNextVersion,
  commitNextVersion,
  pushChanges)
