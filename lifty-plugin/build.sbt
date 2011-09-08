sbtPlugin := true

version := "1.7-RC0-LOCAL"

name := "lifty"

organization := "org.lifty"

scalaVersion := "2.8.1"

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.6.0-SNAPSHOT"

resolvers += "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"