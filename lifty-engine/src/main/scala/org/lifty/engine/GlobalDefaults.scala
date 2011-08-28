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
  
  def replace(str: String) = str match {
    case Pattern("PACKAGE",rest) => searchForMainPackage().map(_+rest).getOrElse({ // screw IO ;) 
      println("Couldn't find a build.properties file so can't supply a default value for the main package")
      ""
    })
    case _ => str
  }
  
  private def searchForMainPackage(): Option[String] = {
    val properties = new Properties()
    properties.load(new FileInputStream("project"+File.separator+"build.properties"))
    properties.getProperty("project.organization") match {
      case null => None
      case str => Some(str)
    }
  }
  
}