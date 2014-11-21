import scalariform.formatter.preferences._

name := "scala-scraper"

organization := "net.ruippeixotog"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.github.nscala-time"     %% "nscala-time"     % "1.4.0",
  "com.typesafe"                % "config"          % "1.2.1",
  "org.jsoup"                   % "jsoup"           % "1.8.1",
  "org.scalaz"                 %% "scalaz-core"     % "7.1.0",
  "junit"                       % "junit"           % "4.11"   % "test",
  "org.specs2"                 %% "specs2"          % "2.4.9"  % "test")

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value.
  setPreference(AlignParameters, true)

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds")

publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

homepage := Some(url("https://github.com/ruippeixotog/scala-scraper"))

pomExtra :=
  <scm>
    <url>https://github.com/ruippeixotog/scala-scraper</url>
    <connection>scm:git:https://github.com/ruippeixotog/scala-scraper.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ruippeixotog</id>
      <name>Rui Gonçalves</name>
      <url>http://ruippeixotog.net</url>
    </developer>
  </developers>
