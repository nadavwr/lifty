package org.lifty

import java.net.{ URL }
import java.io.{ File }
import org.lifty.engine._
import sbt._
import Keys._
import xsbti.{Logger}

object Lifty extends Plugin {

  override lazy val settings = Seq(commands += liftyCommand)

  lazy val liftyCommand = Command.args("lifty","<help>") { (state, args) =>
     
    val scalaCompilerPath = state.configuration.provider.scalaProvider.compilerJar.getPath
    val scalaLibraryPath = state.configuration.provider.scalaProvider.libraryJar.getPath
    val scalatePath = "/Users/Mads/.ivy2/cache/org.fusesource.scalate/scalate-core/bundles/scalate-core-1.4.1.jar"
      
    val cp = (scalaLibraryPath :: scalaCompilerPath :: scalatePath :: Nil).mkString(File.pathSeparator)
      
    new LiftyInstance(Some(cp)).run(args.toList).fold(
      e => {
        println("\n" + e.message + "\n")
        state.fail
      },
      s => {
        println("\n"+s+"\n")
        state
      }
    )
  }
}
