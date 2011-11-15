sbtPlugin := true

version := "1.7"

name := "lifty"

organization := "org.lifty"

scalaVersion := "2.9.1"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / "dev" / ".nexus_credentials")

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.3"