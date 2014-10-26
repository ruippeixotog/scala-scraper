name := "scala-scraper"

organization := "net.ruippeixotog"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.github.nscala-time"     %% "nscala-time"     % "1.4.0",
  "com.typesafe"                % "config"          % "1.2.1",
  "org.jsoup"                   % "jsoup"           % "1.8.1",
  "org.scalaz"                 %% "scalaz-core"     % "7.1.0")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds")
