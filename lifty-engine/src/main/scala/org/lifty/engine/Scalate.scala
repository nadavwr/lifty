package org.lifty.engine

import java.io.{ File, StringWriter, PrintWriter }
import org.lifty.engine.io.{ FileUtil, Storage }
import org.fusesource.scalate.{ TemplateEngine, DefaultRenderContext }

object Scalate {

  import org.lifty.engine.io.FileUtil.{ readToString, writeToFile, writeToTempFile, file}
  import Functional._
  import Util.{ properPath, packageAsPath }

  def run(env: Environment, description: Description, e: Option[TemplateEngine]): String = {
    
    val engine = e.getOrElse {
      val e = new TemplateEngine()
      e.allowCaching = false 
      e
    }
    
    val isRenderable = (file: TemplateFile) => file.file.endsWith(".ssp") // TODO: add support for other template languages
    
    val files = env.template.files ::: description.dependenciesOfTemplate(env.template).flatMap(_.files)
    
    val toRender = files.filter( isRenderable ) 
    val toCopy   = files.filterNot( isRenderable )
    
    // toRender.map ( processTemplate )
    toRender.foreach { file => 
      println("Rendering: " + file.file)
      processTemplate(file,env, description, engine) 
    }
    
    toCopy.foreach { file => 
      println("Copying file: " + file.file)
      copyTemplate(file,env)
    }
    
    invalidInjections(env.template, description).foreach { templateInjection => 
      for {
        txtFile     <- Storage.template(env.recipe, templateInjection.file).unsafePerformIO
        contents    <- FileUtil.readToString(txtFile).unsafePerformIO
        tempFile    <- writeToTempFile(contents)
        renderedStr <- Some(render(tempFile, env, engine))
      } {
        println("\nWasn't able to inject\n\n%s\n\ninto %s at %s ".format(
          renderedStr,
          templateInjection.into,
          templateInjection.point
        ))
      }
    }
    
    for {
      path <- description.allFolders(env.template)
      f = replaceVariables(path, env) |> properPath |> file
    } f.mkdirs()
    
    "Done."
  }
  
  /*
   *
   * Methods related to processing the Scalate templates. 
   *
   */

  private def copyTemplate(template: TemplateFile, env: Environment): (TemplateFile, Boolean) = {
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile => 
      val destination = replaceVariables(template.destination, env) |> properPath |> file
      for {
        templateStr <- readToString(templateFile).unsafePerformIO
        result      <- writeToFile(templateStr, destination)
      } yield (template, true)
    } getOrElse (template, false)
  }

  /** 
   * Processes a single TemplateFile.  
   * 
   * @param template  The TemplateFile to process
   * @param env       The environment in which the template was invoked (i.e. CLI arguments etc.)
   */
  private def processTemplate(template: TemplateFile, 
                              env: Environment, 
                              description: Description,
                              engine: TemplateEngine): (TemplateFile, Boolean) = {
    
    val destination = replaceVariables(template.destination, env) |> properPath |> file 
    (for {
      renderedStr <- processTemplateInMemory(template, env, description, engine)
      result      <- writeToFile(renderedStr, destination)
    } yield (template, true) ).getOrElse(template,false)
  }
  
  private def processTemplateInMemory(template: TemplateFile, 
                                      env: Environment, 
                                      description: Description,
                                      engine: TemplateEngine): Option[String] = {
                                        
    Storage.template(env.recipe, template.file).unsafePerformIO.flatMap { templateFile => 
      for {
        templateStr <- readToString(templateFile).unsafePerformIO
        injectedStr <- Some(inject(templateStr, template, env, description))
        tempFile    <- writeToTempFile(injectedStr)
        renderedStr <- Some(render(tempFile, env, engine))
      } yield Some(renderedStr)
    } getOrElse None
  }
  
  /*
   *
   * Methods related to adding the injections into the templates. 
   *
   */
    
  private def inject(rawTemplate: String, 
                     templateFile: TemplateFile, 
                     env: Environment, 
                     description: Description): String = {
    
    val regxp = """\/{2}\#inject\spoint\:\s(\w*)""".r
    
    rawTemplate.split("\n").map { line => 
      if (!regxp.findFirstIn(line).isEmpty) {
        val point = regxp.findFirstMatchIn(line).get.group(1)
        injectionsForPointInFile(point, templateFile, description, env.template).map { injection => 
          Storage.template(env.recipe, injection.file).unsafePerformIO.flatMap { injectionFile =>
            readToString(injectionFile).unsafePerformIO
          }.getOrElse {
            println("Tried to load " + injection.file + " but failed")
            ""
          }
        }.mkString("\n") // Adding newlines between injections. 
      } else line 
    }.mkString("\n") // joining all the lines together again.
  }
  
  private def injectionsForPointInFile(point: String, 
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
  
  /*
   *
   * Misc. 
   *
   */

  /**
   * Given a String it will replace all variables (i.e. ..}) with the proper values that
   * were supplied by the user when invoking the template. 
   */
  private def replaceVariables(str: String, env: Environment): String = {
    
    def findAndTransformValueForArgument(name: String) = packageAsPath(env.values(name))
    
    """\$\{\w*\}""".r.findAllIn(str).toList match {
      case Nil => str
      case xs  => xs.map(_.toString).foldLeft(str){ (p,s) => 
        val argName = s.replace("${","").replace("}","") //TODO: make prettier?
        p.replace(s, findAndTransformValueForArgument(argName))
      }
    }
  }
  
  /**
   * Adds arguments to the context. 
   */
  private def addArgumentsToContext(context: DefaultRenderContext, env: Environment): Unit = {
    env.values.foreach { kv =>
      val (key,value) = kv
      context.attributes(key) = value
    }
  }
  
  private def render(file: File, env: Environment, engine: TemplateEngine): String = {
    val buffer  = new StringWriter()
    val context = new DefaultRenderContext("",engine, new PrintWriter(buffer))
    addArgumentsToContext(context, env)
    engine.load(file).render(context) // this writes to the buffer
    buffer.toString
  }

}