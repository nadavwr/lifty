package org.lifty.engine

import org.lifty.engine.io.{ FileUtil, Storage }
import org.lifty.engine.io.FileUtil.{ readToString, writeToFile, file}
import Functional._
import Util.{ properPath, packageAsPath }

import scalaz._
import scalaz.effects._
import Scalaz._

object TemplateRenderer {

  /* TODO: MUCH BETTER ERROR HANDLING */

  def run(env: Environment, description: Description): Validation[Error,String] = {

    val isRenderable = (file: TemplateFile) => file.file.endsWith(".ssp") // TODO: CHANGE TO SOMETHING ELSE AT SOME POINT
    val files        = env.template.files ::: description.dependenciesOfTemplate(env.template).flatMap(_.files)
    val toRender     = files.filter( isRenderable )
    val toCopy       = files.filterNot( isRenderable )

    // create folder structure
    for {
      path <- description.allFolders(env.template)
      f = replaceVariables(path, env) |> properPath |> file
    } f.mkdirs()

    // render and copy templates (in parallel, yeah!)
    toRender.foreach { f => renderTemplate(f,env,description) }
    toCopy.par.foreach { f => copyTemplate(f,env) }
    
    // for any injections that wasn't possible, tell to user how to continue
    invalidInjections(env.template, description).foreach { templateInjection => 
      for {
        txtFile     <- Storage.template(env.recipe, templateInjection.file).unsafePerformIO
        contents    <- FileUtil.readToString(txtFile).unsafePerformIO
        renderedStr <- Some(renderString(contents, None, env, description))
      } {
        println("\nWasn't able to inject\n\n%s\n\ninto %s at %s ".format(
          renderedStr,
          templateInjection.into,
          templateInjection.point
        ))
      }
    }
    
    /* TODO: MUCH BETTER ERROR HANDLING */
    "Done.".success
  }

  private def renderString(str: String, templateOpt: Option[TemplateFile], env: Environment, description: Description): String = {
    
    def processLine(line: String): String = {
      
      println("processLine")
      
      val variable  = """\$\{(\w*)\}""".r                   //example: ${argumentName}
      val injection = """\/{2}\#inject\spoint\:\s(\w*)""".r //example: //#inject point: dependencies

      val variablesReplaced = variable.replaceAllIn(line, m => env.values(m.group(m.groupCount)))
      
      (for { template <- templateOpt } yield {
        
        val injectionsReplaced = injection.replaceAllIn(variablesReplaced, m => {
          
          val point = m.group(m.groupCount)
          injectionsForPointInFile(point, template, description, env.template).map { injection => 
            Storage.template(env.recipe, injection.file).unsafePerformIO.flatMap { injectionFile =>
              (for { 
                read <- readToString(injectionFile).unsafePerformIO
              } yield processLine(read))
            }.getOrElse {
              println("Tried to load " + injection.file + " but failed") // Propagate to Validation
              ""
            }
          }.mkString("\n")
        })
      
        injectionsReplaced
      }) getOrElse(variablesReplaced)
    }
    
    str.split("\n").map(processLine).mkString("\n")
  }

  private def renderTemplate(template: TemplateFile, env: Environment, description: Description): String = {

    val destination = replaceVariables(template.destination, env) |> properPath |> file
    
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile =>
      for {
        str      <- readToString(templateFile).unsafePerformIO
        rendered = renderString(str, Some(template), env, description)
        result   <- writeToFile(rendered, destination)
      } yield "Rendered file to " + destination
    } getOrElse "Wasn't able to render a file to " + destination
  }

  private def copyTemplate(template: TemplateFile, env: Environment): String = {
    
    val destination = replaceVariables(template.destination, env) |> properPath |> file
    
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile =>
      for {
        templateStr <- readToString(templateFile).unsafePerformIO
        result      <- writeToFile(templateStr, destination)
      } yield "Copied file " + template.file
    } getOrElse "Wasn't able to copy a file to " + destination
  }

  //
  // Util
  //

  private def injectionsForPointInFile(point: String, templateFile: TemplateFile, description: Description, rendering: Template): List[TemplateInjection] = { 
    val dependencies = description.dependenciesOfTemplate(rendering)
    val injections   = dependencies.flatMap { _.injections } ::: rendering.injections 
    injections.filter( _.into == templateFile.file )
              .filter( _.point == point)
  }
  
  private def invalidInjections(template: Template, description: Description): List[TemplateInjection] = {
    val dependencies    = description.dependenciesOfTemplate(template)
    val injections      = dependencies.flatMap { _.injections } ::: template.injections 
    val files           = template.files.map(_.file) ::: dependencies.flatMap(_.files).map(_.file)
    val validInjections = injections.filter( injection => files.contains(injection.into))
    injections filter ( injection => !validInjections.contains(injection))
  }

  // Replaces variables in the descriptor.json with their proper values. 
  private def replaceVariables(str: String, env: Environment): String = {

    def findAndTransformValueForArgument(name: String) = packageAsPath(env.values(name))
    
    val variable = """\$\{(\w*)\}""".r
    
    variable.replaceAllIn(str, m => findAndTransformValueForArgument(m.group(1)) )
    
  }

}