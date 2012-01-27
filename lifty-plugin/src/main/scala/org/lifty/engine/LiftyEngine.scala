package org.lifty.engine

import org.lifty.engine.io._
import java.net.{ URL, URI }
import scalaz._
import scalaz.effects._
import Scalaz._

object LiftyEngine {

  def create(recipe: String, templateName: String, config: LiftyConfiguration): Validation[Error,String] = {
    descriptionOfRecipe(recipe).flatMap { description =>
      val template = templateOfRecipe(description, templateName)
      requestArguments(recipe,template,description).flatMap { env =>
        TemplateRenderer.run(env, description, config)
      }
    }
  }

  def templates(recipe: String): Validation[Error, String] = {
    descriptionOfRecipe(recipe).flatMap { description =>
      val max = description.templates.map(_.name.length).max

      (for {
        template <- description.templates
        output = "%s%s%s".format(
          template.name,
          (0 to (max - template.name.length + 3)).map( _ => " ").mkString, // 3 for extra spacing
          template.description)
      } yield output).mkString("\n").success
    }
  }

  def learn(name: String, url: String): Validation[Error, String] = {
    Storage
      .storeRecipe(name, new URL(url)).unsafePerformIO
      .map( recipe => learnMsg(name,url, recipe) )
  }

  def delete(recipe: String): Validation[Error, String] = {
    Storage.deleteRecipe(recipe).unsafePerformIO
  }

  def upgrade(recipe: String): Validation[Error, String] = {
    Storage.recipe(recipe).unsafePerformIO.flatMap( s => 
      DescriptionLoader.load(s.descriptor).unsafePerformIO.fold(
          (e) => Error("Wasn't able to parse the local .json file. Please uninstall and re-learn the recipe.").fail,
          (s) => {
            val origin = new URL(s.origin)
            val version = s.version
            DescriptionLoader.load(origin).unsafePerformIO.fold(
              (e) => e.fail,
              (s) => if (s.version > version) {
                Storage.storeRecipe(recipe, origin).unsafePerformIO.fold(
                  (e) => e.fail,
                  (s) => "Successfully updated the recipe.".format(version).success
                )
              } else {
                "You have the most recent version of the recipe installed.".success
              }
    )}))
  }

  def recipes(): Validation[Error, String] = {
    (
    "The following recipes are installed: " ::
    "" ::
    Storage.allRecipes.unsafePerformIO.map(_.name).mkString("\n") :: Nil).mkString("\n").success
  }

  def help(): Validation[Error, String] = {
    (
    "help                         Shows this message" ::
    "create <recipe> <template>   Create a template from the given recipe" ::
    "templates <recipe>           List all the templates defined by the recipe" ::
    "learn <name> <url>           Learn the recipe at the given URL and store it locally under the given name" ::
    "delete <name>                Deletes a recipe. " ::
    "recipes                      Lists all installed recipes" ::
    "update <recipe>              Update the recipe if a new version exists" :: Nil).mkString("\n").success
  }

  private def descriptionOfRecipe(recipeName: String): Validation[Error,Description] = {
    Storage.recipe(recipeName).unsafePerformIO.flatMap { recipe =>
      DescriptionLoader.load(recipe.descriptor).unsafePerformIO
    }
  }

  private def templateOfRecipe(description: Description, templateName: String): Template = {
    description.templates.filter( _.name == templateName ).head
  }

  def learnMsg(name: String, url: String, recipe: Recipe) = {
    "Lifty successfully installed recipe with name: '%s'\n".format(name) +
    "\n"+
    "Run 'lifty templates %s' for information about\n".format(name) + 
    "the newly installed templates. Happy hacking." 
  }

  /**
   * Will request values for each for the arguments of the template and dependent templates.
   */
  def requestArguments(recipe: String, template: Template, description: Description): Validation[Error, Environment] = {

    val Pattern = """\$\{(.*)\}(.*)""".r

    // request input for an argument that is missing
    val requestInputForMissingArgument = (previous: Map[String,String], argument: Argument) => {
      // see if there's a previous value to replace
      val default = argument.default.map{ dflt =>

        (for { matched <- Pattern.findFirstMatchIn(dflt)
              name = matched.group(1)
              rest = matched.group(2)
              tobe <- previous.get(name)
            } yield tobe+rest) getOrElse dflt


      }.getOrElse("")
      val value = InputReaderComponent
                    .requestInput("%s%s: ".format(argument.descriptiveName, argument.default.map(_=>" ["+default+"]").getOrElse("")),default)
                    .unsafePerformIO
      (argument.name, value)
    }

    val allArgs = description.allArguments(template)
    
    if (!allArgs.isEmpty) {
      println("\nHi, I just need some more information so I can\n" +
                 "create the templates just the way you want them\n")
    }

    val kvs = allArgs.foldLeft(Nil: List[(String,String)]) { (xs,c) =>
      requestInputForMissingArgument(Map(xs: _*), c) :: xs
    }

    Environment(recipe, template, Map(kvs: _*)).success

  }
}