version := "0.7"

name := "lifty-engine"

organization := "org.lifty"

scalaVersion := "2.9.0-1"

resolvers += "Scala Tools Releases" at "http://scala-tools.org/repo-releases/"

libraryDependencies += "org.scalatest" % "scalatest" % "1.3" % "test"

libraryDependencies += "net.liftweb" % "lift-json_2.9.0-1" % "2.4-M3"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.1"

libraryDependencies += "jline" % "jline" % "0.9.94"

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.1"