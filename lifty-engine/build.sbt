version := "0.7.4"

name := "lifty-engine"

organization := "org.lifty"

scalaVersion := "2.9.1"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

credentials += Credentials(Path.userHome / "dev" / ".nexus_credentials")

resolvers += "Scala Tools Releases" at "http://scala-tools.org/repo-releases/"

libraryDependencies += "org.scalatest" % "scalatest" % "1.3" % "test"

libraryDependencies += "net.liftweb" %% "lift-json" % "2.4-M5"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.3"

libraryDependencies += "jline" % "jline" % "0.9.94"

pomPostProcess := {
    import xml._
    Rewrite.rewriter {
        case e: Elem if e.label == "classifier" => NodeSeq.Empty
    }
}