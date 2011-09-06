// 
//  GlobalDefaults.scala
//  lifty_engine
//  
//  Created by Mads Hartmann Jensen on 2011-08-28.
// 

package org.lifty.engine

import java.util.Properties
import java.io.{ File, FileInputStream }

/*
 * 
 */
object GlobalDefaults {
  
  
  val Pattern = """\$\{(.*)\}(.*)""".r
  
  def replace(str: String) = {
  
    def f(opt: Option[String], other: String) = opt.map(_+other).getOrElse({
      println("Couldn't find a build.properties file so can't supply a default value for the argument")
      ""
    })
    
    str match {
      case Pattern("PACKAGE",rest)       => f(searchForMainPackage(), rest)
      case _                             => str
    }
  }
  
  private def searchForMainPackage(): Option[String] = search("project.organization")
  
  private def searchForScalaVersion(): Option[String] = search("build.scala.versions")
  
  private def searchForProjectName(): Option[String] = search("project.name")
  
  private def search(property: String) = {
    val properties = new Properties()
    properties.load(new FileInputStream("project"+File.separator+"build.properties"))
    properties.getProperty(property) match {
      case null => None
      case str => Some(str)
    }
  }
}