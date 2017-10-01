import ReleaseTransformations._
import scalariform.formatter.preferences._

organization in ThisBuild := "net.ruippeixotog"

scalaVersion in ThisBuild := "2.12.2"
crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.2")

lazy val core = project.in(file("core"))
  .enablePlugins(TutPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper",

    libraryDependencies ++= Seq(
      "com.github.nscala-time"     %% "nscala-time"          % "2.16.0",
      "net.sourceforge.htmlunit"    % "htmlunit"             % "2.26",
      "org.jsoup"                   % "jsoup"                % "1.10.2",
      "org.scalaz"                 %% "scalaz-core"          % "7.2.12",
      "org.http4s"                 %% "http4s-blaze-server"  % "0.15.11a"             % "test",
      "org.http4s"                 %% "http4s-dsl"           % "0.15.11a"             % "test",
      "org.slf4j"                   % "slf4j-nop"            % "1.7.25"               % "test",
      "org.specs2"                 %% "specs2-core"          % "3.8.9"                % "test"),

    tutTargetDirectory := file("."))

lazy val config = project.in(file("modules/config"))
  .dependsOn(core)
  .enablePlugins(TutPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "scala-scraper-config",

    libraryDependencies ++= Seq(
      "com.typesafe"                % "config"               % "1.3.1",
      "org.specs2"                 %% "specs2-core"          % "3.8.9"                % "test"))

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"),

  scalariformPreferences := scalariformPreferences.value
    .setPreference(DanglingCloseParenthesis, Prevent)
    .setPreference(DoubleIndentConstructorArguments, true)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true),

  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-Ypartial-unification"),

  fork in Test := true,

  tutTargetDirectory := baseDirectory.value,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },

  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
  homepage := Some(url("https://github.com/ruippeixotog/scala-scraper")),
  pomExtra := {
    <scm>
      <url>https://github.com/ruippeixotog/scala-scraper</url>
      <connection>scm:git:https://github.com/ruippeixotog/scala-scraper.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ruippeixotog</id>
        <name>Rui Gonçalves</name>
        <url>http://www.ruippeixotog.net</url>
      </developer>
    </developers>
  },
  releasePublishArtifactsAction := PgpKeys.publishSigned.value)

releaseCrossBuild := true
releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"
