sbtPlugin := true

version := "1.7.4"

name := "lifty"

organization := "org.lifty"

scalaVersion := "2.9.1"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

credentials += Credentials(Path.userHome / "dev" / ".nexus_credentials")

pomPostProcess := {
    import xml._
    Rewrite.rewriter {
        case e: Elem if e.label == "classifier" => NodeSeq.Empty
    }
}