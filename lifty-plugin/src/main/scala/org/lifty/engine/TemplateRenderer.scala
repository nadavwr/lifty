package org.lifty.engine

import scala.util.matching.Regex._

import org.lifty.engine.io.{ FileUtil, Storage }
import org.lifty.engine.io.FileUtil.{ readToString, writeToFile, file}
import Functional._
import Util.{ properPath, packageAsPath }

import scalaz._
import scalaz.effects._
import Scalaz._

object TemplateRenderer {

  /* TODO: MUCH BETTER ERROR HANDLING */

  def run(
    env: Environment, 
    description: Description, 
    config: LiftyConfiguration): Validation[Error,String] = {

    val isRenderable = (file: TemplateFile) => file.file.endsWith(".ssp") // TODO: CHANGE TO SOMETHING ELSE AT SOME POINT
    val files        = env.template.files ::: description.dependenciesOfTemplate(env.template).flatMap(_.files)
    val toRender     = files.filter( isRenderable )
    val toCopy       = files.filterNot( isRenderable )

    // create folder structure
    for {
      path <- description.allFolders(env.template)
      f = config.folderName + "/" + replaceVariables(path, env) |> properPath |> file
    } f.mkdirs()

    // render and copy templates (in parallel, yeah!)
    val (rendered, copied) = (
      toRender.par.map { f => renderTemplate(f,env,description, config) },
      toCopy.par.map { f => copyTemplate(f,env, config) }
    )
    
    val failedRendered = rendered.filter( _.isFailure )
    val failedCopied   = copied.filter( _.isFailure )
    
    if (!failedRendered.isEmpty || !failedCopied.isEmpty) {
      
      Error((failedRendered.map( _.fold(e => e.message , s => s) ) ++
             failedCopied.map( _.fold(e => e.message , s => s ))).mkString("\n")).fail
      
      
    } else { // no failures, we can continue. 
      
      val invalid = invalidInjections(env.template, description)
      
      if (!invalid.isEmpty) {
        
        val injectionMsg: List[String] = (for {
          injection <- invalid
        } yield for {
          txtFile     <- Storage.template(env.recipe, injection.file).unsafePerformIO
          contents    <- FileUtil.readToString(txtFile).unsafePerformIO
          renderedStr <- renderString(contents, None, env, description).toOption
        } yield {
          "\nWasn't able to inject the following into %s at %s\n\n%s\n\n".format(
            injection.into,
            injection.point,
            renderedStr
          )
        }).flatten
        
        (injectionMsg.mkString("\n") +
        "I successfully finished, however, some of the templates I\n"   + 
        "rendered wanted to inject code into existing files and I'm\n"  +
        "not allowed to do that so you have to do it manually, sorry.\n" + 
        "Read above to see what I wanted to inject into each file.").success
        
      } else {
        ("I successfully finished." + 
          env.template.notice.map(s => " I was asked to tell you to: \n\n" + s).getOrElse("")).success
      }
    }
  }

  private def renderString(
    str: String, 
    templateOpt: Option[TemplateFile], 
    env: Environment, 
    description: Description): Validation[Error,String] = {

    def processLine(line: String): Validation[Error,String] = {

      val variable  = """\$\{(\w*)\}""".r                   //example: ${argumentName}
      val injection = """\/{2}\#inject\spoint\:\s(\w*)""".r //example: //#inject point: dependencies

      val variablesReplaced = variable.replaceAllIn(line, m => env.values(m.group(m.groupCount)))
      
      (for { 
        template <- templateOpt 
      } yield {
        
        def grp(m: Match) = {
          val point = m.group(m.groupCount)
          injectionsForPointInFile(point, template, description, env.template).map { injection => 
            Storage.template(env.recipe, injection.file).unsafePerformIO.flatMap { injectionFile =>
              (for { 
                read <- readToString(injectionFile).unsafePerformIO
                processed <- processLine(read).toOption
              } yield processed )
            }.getOrElse {
              throw new Exception("Tried to load " + injection.file + " but failed")
            }
          }.mkString("\n")
        }
        
        try { 
          injection.replaceAllIn(variablesReplaced, grp(_)).success
        } catch {
          case e: Exception => Error(e.getMessage).fail
        }

      }).getOrElse(variablesReplaced.success)
    }
    
    val processed = str.split("\n").map(processLine)
    val processedFailed = processed.filter(_.isFailure)
    
    if (processedFailed.isEmpty) {
      processed.flatMap(_.toOption).mkString("\n").success
    } else {
      Error(processedFailed.map( _.fold(e => e.message , s => s)).mkString("\n")).fail
    }
  }

  private def renderTemplate(
    template: TemplateFile, 
    env: Environment, 
    description: Description, 
    config: LiftyConfiguration): Validation[Error,String] = {

    val destination = config.folderName + "/" + replaceVariables(template.destination, env) |> properPath |> file
    
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile =>
      for {
        str      <- readToString(templateFile).unsafePerformIO
        rendered <- renderString(str, Some(template), env, description).toOption
        result   <- writeToFile(rendered, destination)
      } yield ("Rendered file to " + destination).success
    } getOrElse Error("Wasn't able to render a file to " + destination).fail
  }

  private def copyTemplate(
    template: TemplateFile, 
    env: Environment,
    config: LiftyConfiguration): Validation[Error,String] = {
    
    val destination = config.folderName + "/" + replaceVariables(template.destination, env) |> properPath |> file
    
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile =>
      for {
        templateStr <- readToString(templateFile).unsafePerformIO
        result      <- writeToFile(templateStr, destination)
      } yield ("Copied file " + template.file).success
    } getOrElse Error("Wasn't able to copy a file to " + destination).fail
  }

  //
  // Util
  //

  private def injectionsForPointInFile(
    point: String, 
    templateFile: TemplateFile, 
    description: Description, 
    rendering: Template): List[TemplateInjection] = { 
      
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