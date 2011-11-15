package org.lifty

import java.net.{ URL }
import java.io.{ File }
import org.lifty.engine._
import sbt._
import Keys._
import sbt.compiler.{ RawCompiler }
import xsbti.{Logger}

import org.fusesource.scalate.{ TemplateEngine }
import org.fusesource.scalate.support.{ Compiler }

object Lifty extends Plugin {

  override lazy val settings = Seq(commands += liftyCommand)

  lazy val liftyCommand = Command.args("lifty","<help>") { (state, args) =>
    
    val bytecodeDir = 
      new java.io.File(List(System.getProperty("user.home"),".lifty_workspace","classes").mkString(File.separator))
    
    bytecodeDir.mkdirs() 
    
    // classpath
    
    val cp = {
      val scalaCompilerPath = state.configuration.provider.scalaProvider.compilerJar
      val scalaLibraryPath = state.configuration.provider.scalaProvider.libraryJar
      val scalatePath = new java.io.File(List(
        System.getProperty("user.home"),
        ".ivy2",
        "cache",
        "org.fusesource.scalate",
        "scalate-core",
        "bundles",
        "scalate-core-1.6.0-SNAPSHOT.jar"
      ).mkString(File.separator))
      val scalateUtilPath = new java.io.File(List(
        System.getProperty("user.home"),
        ".ivy2",
        "cache",
        "org.fusesource.scalate",
        "scalate-util",
        "bundles",
        "scalate-util-1.6.0-SNAPSHOT.jar"
      ).mkString(File.separator))
      (scalaLibraryPath :: scalaCompilerPath :: scalatePath :: scalateUtilPath :: Nil)
    }
    
    //  SBTCompiler
    class SBTCompiler extends Compiler {
      
      val cpopts = (new ClasspathOptions(bootLibrary = false, 
                                         compiler = true, 
                                         extra = true, 
                                         autoBoot = false,
                                         filterLibrary = false)): ClasspathOptions
      
      def compile(file: File): Unit = {
        
        val scalaProvider = state.configuration.provider.scalaProvider
        val scalaInstance = new ScalaInstance(version = scalaProvider.version, 
                                              explicitActual = None, /* not sure if this is correct.*/
                                              libraryJar = scalaProvider.libraryJar, 
                                              compilerJar = scalaProvider.compilerJar, 
                                              loader = scalaProvider.loader,
                                              extraJars = scalaProvider.jars)

        val compiler = new RawCompiler(scalaInstance = scalaInstance, 
                                       cp = cpopts, 
                                       log = ConsoleLogger())
        
        compiler.apply(sources = List(file),
                       classpath = cp,
                       outputDirectory = bytecodeDir,
                       options = Nil)
      }
    }
    
    class SBTTemplateEngine() extends TemplateEngine {
      override protected def createCompiler: Compiler = {
        new SBTCompiler()
      }
      override def bytecodeDirectory = bytecodeDir
    } 
     
    // create the special engine. 
    val engine = new SBTTemplateEngine()
    engine.allowCaching = false 
    
    
    // Run the lifty instance.    
    new LiftyInstance(Some(engine)).run(args.toList).fold(
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
