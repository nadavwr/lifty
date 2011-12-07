package org.lifty.engine


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
  arguments: List[Argument],
  sources: List[Source]) {
  
  def templateNamed(name: String): Option[Template] = {
    templates.find( _.name == name)
  }
  
  def argumentNamed(name: String): Option[Argument] = {
    arguments.find( _.name == name)
  }
  
  def allArguments(template: Template): List[Argument] = {
    val directArguments = template.arguments
    val transitive      = dependenciesOfTemplate(template).flatMap( _.arguments)
    (directArguments ::: transitive).distinct.flatMap( (arg: String) => arguments.find( _.name == arg ) )
  }
  
  def allFolders(template: Template): List[String] = {
    
    val thisFolders = template.folders.getOrElse( List[String]() )
    val other = dependenciesOfTemplate(template).flatMap( _.folders ).flatten
    
    (thisFolders ::: other).distinct
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
  folders:        Option[List[String]],
  arguments:      List[String],
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
  name:            String,
  descriptiveName: String, 
  default:         Option[String])

case class TemplateInjection(
  file:         String,
  into:         String,
  point:        String
)
