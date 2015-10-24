import scalariform.formatter.preferences._

name := "scala-scraper"

organization := "net.ruippeixotog"

version := "0.1.2"

scalaVersion := "2.11.7"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "com.github.nscala-time"     %% "nscala-time"     % "2.4.0",
  "com.typesafe"                % "config"          % "1.3.0",
  "org.jsoup"                   % "jsoup"           % "1.8.3",
  "org.scalaz"                 %% "scalaz-core"     % "7.1.4",
  "org.specs2"                 %% "specs2-core"     % "3.6.5"  % "test")

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
      <url>http://www.ruippeixotog.net</url>
    </developer>
  </developers>
