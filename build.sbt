import scalariform.formatter.preferences._

name := "scala-scraper"
organization := "net.ruippeixotog"
version := "1.0.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")

libraryDependencies ++= Seq(
  "com.github.nscala-time"     %% "nscala-time"          % "2.12.0",
  "com.typesafe"                % "config"               % "1.3.0",
  "net.sourceforge.htmlunit"    % "htmlunit"             % "2.20",
  "org.jsoup"                   % "jsoup"                % "1.8.3",
  "org.scalaz"                 %% "scalaz-core"          % "7.2.2",
  "org.http4s"                 %% "http4s-blaze-server"  % "0.13.0a"              % "test",
  "org.http4s"                 %% "http4s-dsl"           % "0.13.0a"              % "test",
  "org.slf4j"                   % "slf4j-nop"            % "1.7.21"               % "test",
  "org.specs2"                 %% "specs2-core"          % "3.7.2"                % "test")

scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Prevent)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)

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
