package org.lifty.engine

import scalaz._
import Scalaz._

/** 
 * This is the component of the Lifty that deals with the parsing of 
 * the input from the user.
 */
trait InputParser {

  
  this: Lifty =>

  /**
   * Given a list of arguments it will (if successful) return the appropriate Command
   * and the rest of the arguments. Otherwise an instance of Error.
   */
  def parseCommand(arguments: List[String]): Validation[Error, (Command, List[String])] = {
    if (arguments.nonEmpty) {
      arguments.head match {
        case TemplatesCommand.keyword       => (TemplatesCommand, arguments.tail).success
        case HelpCommand.keyword            => (HelpCommand, arguments.tail).success
        case CreateCommand.keyword          => (CreateCommand, arguments.tail).success
        case UpdateTemplatesCommand.keyword => (UpdateTemplatesCommand,arguments.tail).success
        case RecipesCommand.keyword         => (RecipesCommand, Nil).success
        case LearnCommand.keyword           => (LearnCommand, arguments.tail).success
        case DeleteCommand.keyword          => (DeleteCommand, arguments.tail).success
        case command                        => Error("No command named %s".format(command)).fail
      }
    } else { Error("You have to supply an argument").fail }
  }

  /**
   * Given a list of arguments it will (if successful) return the appropriate Template
   * and the rest of the arguments. Otherwise an instance of Error
   */
  def parseTemplate(recipeDescription: Description, arguments: List[String]): Validation[Error, (Template, List[String])] = {
    if (arguments.nonEmpty) {
      (for {
        template <- recipeDescription.templates.filter(_.name == arguments.head).headOption
      } yield (template, arguments.tail).success)
        .getOrElse(Error("No template with the name %s".format(arguments.head)).fail)
    } else { Error("You have to write the name of the template").fail }
  }

  /**
   * Will request values for each for the arguments of the template and dependent templates. 
   */
  def parseArguments(recipe: String, template: Template, description: Description): Validation[Error, Environment] = {

    // request input for an argument that is missing
    val requestInputForMissingArgument = (argument: Argument) => {
      val default = argument.default.map(GlobalDefaults.replace(_)).getOrElse("")
      val value = this.inputComponent
                      .requestInput("Enter value for %s%s: ".format(argument.name, argument.default.map(_=>"["+default+"]").getOrElse("")),default)
                      .unsafePerformIO // TODO: Performing IO
      (argument.name, value) // TODO: This is a side-effect. IO-Monad?
    }
    
    val kvs = description.allArguments(template).map(requestInputForMissingArgument(_))
    Environment(recipe, template, Map(kvs: _*)).success

  }

}
