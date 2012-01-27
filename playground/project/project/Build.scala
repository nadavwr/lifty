import sbt._
import Keys._ 

object Playground extends Build {
  
  val plugin = RootProject(file("../../lifty-plugin"))
  
  val root = Project("playground", file(".")).dependsOn(plugin)
  
}