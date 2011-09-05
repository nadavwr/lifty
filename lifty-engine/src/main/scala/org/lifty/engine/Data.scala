package org.lifty.engine

/*
  The following are the data classes that are used internally
*/

trait Command { val keyword: String }
object HelpCommand extends Command { val keyword = "help"}                       
object CreateCommand extends Command { val keyword = "create"}                   
object TemplatesCommand extends Command { val keyword = "templates"}             
object UpdateTemplatesCommand extends Command { val keyword = "update"}
object LearnCommand extends Command { val keyword = "learn" }
object RecipesCommand extends Command { val keyword = "recipes" }

case class Error(message: String)

case class Environment(recipe: String, template: Template, values: Map[String,String])

/*
  The following are the case classes needed to describe a Lifty-engine application
  in JSON
*/

case class Description(
  origin: String,
  version: Int, 
  templates: List[Template],
  sources: List[Source]) {
  
  
  def templateNamed(name: String): Option[Template] = {
    templates.find( _.name == name)
  }
  
  def allArguments(template: Template): List[Argument] = {
    val directArguments = template.arguments
    val transitive      = dependenciesOfTemplate(template).flatMap( _.arguments)
    (directArguments ::: transitive).distinct
  }
  
  /** 
   * Get the dependencies (also the transitive ones) of a Template
   */
  def dependenciesOfTemplate(template: Template): List[Template] = {    
    val directDependencies = template.dependencies.flatMap { templateNamed(_) }
    val transitive         = directDependencies.flatMap { dependenciesOfTemplate(_) }
    directDependencies ::: transitive  
  }
  
}

case class Template(
  name:           String,
  description:    String,
  notice:         Option[String],
  arguments:      List[Argument],
  files:          List[TemplateFile],
  injections:     List[TemplateInjection],
  dependencies:   List[String])

case class Source(
  name:           String, 
  url:            String
)

case class TemplateFile(
  file:           String,   // The scalate template to render
  destination:    String    // Where to create a file with the result
)

case class Argument(
  name:         String,
  default:      Option[String],
  repeatable:   Option[Boolean])

case class TemplateInjection(
  file:         String,
  into:         String,
  point:        String
)
