//
//  Storage.scala
//  Lifty engine
//
//  Created by Mads Hartmann Jensen on 2011-06-16.

package org.lifty.engine.io

import scalaz._
import scalaz.effects._
import Scalaz._
import java.net.{ URL }
import java.io.{ File }
import org.lifty.engine.{ Error, Description, Template }
import Downloader.{ download }
import DescriptionLoader.{ load }
import org.lifty.engine.io.FileUtil.{ file }

case class Recipe(name: String, descriptor: File, templates: Seq[File])

/*
  Storage component. It's used to download, store and fetch the local versions of the
  different recipes. The structure of the storage is as follows:

  <pre>
  ~/.lifty
    recipe name
      recipe name.json
      template1.ssp
      template2.ssp
  </pre>
*/
object Storage {

  val root = file(System.getProperty("user.home"))

  lazy val storage = {
    val f = file(root.getAbsolutePath + / + ".lifty") 
    if (!f.exists()) {
      f.mkdirs()
    }
    f
  }

  /*
   *
   * Related to storing/fetching/deleting recipes
   * 
   */

  /** 
   * Attempts to fetch a recipe from the storage 
   * 
   * @param name The name of the recipe
   */
  def recipe(name: String): IO[Validation[Error, Recipe]] = io {
    (for {
      folder     <- storage.listFiles.filter( f => f.isDirectory && f.getName == name ).headOption
      descriptor <- folder.listFiles.filter( f => f.isFile && f.getName == name+".json").headOption
    } yield {
      val templates = folder.listFiles.filter( f => f.isFile && f.getName.endsWith(".ssp"))
      Recipe(name, descriptor, templates).success
    }).getOrElse(Error("No recipe named %s in the storage.".format(name)).fail)
  }

  /** 
   * Attempts to store a recipe at the given url in the storage under the given name. 
   * 
   * @param name The name of the recipe
   * @param url The URL of the recipe's .json description file
   */
  def storeRecipe(name: String, url: URL): IO[Validation[Error, Recipe]] = io {
    
    recipe(name).unsafePerformIO.fold(
      (e) => {
        val recipe = file(List(storage.getAbsolutePath, name, name+".json").mkString(/))

        download(url, recipe).unsafePerformIO.fold( err => err.fail,
          file => load(file).unsafePerformIO.fold( err => err.fail,
            description => Recipe(name, file, storeSourcesOfDescription(name, description)).success
          )
        )
      },
      (s) => Error("A recipe with that name already exists. use 'lifty update %s' to update it.".format(name)).fail
    )
  }
    
  /** 
   * Returns a list with all of the recipes currently in the storage.  
   */
  def allRecipes: IO[List[Recipe]] = io {
    storage.listFiles
           .filter( _.isDirectory)
           .map( f => recipe(f.getName).unsafePerformIO )
           .filter( _.isSuccess )
           .map( _.toOption.get )
           .toList
  }
  
  /**
   * Returns a list of all of the names of the templates of the specific recipe.
   */
  def templateNames(name: String): IO[Validation[Error,List[String]]] = {
    recipe(name).map{ i => i.flatMap { r => 
      DescriptionLoader.load(r.descriptor).unsafePerformIO.map{ description => description.templates.map(_.name )}
    }}
  }


  /** 
   * Deletes a recipe from the store 
   * 
   * @param name The name of the recipe
   */
  def deleteRecipe(name: String): IO[Validation[Error, String]] = io {
    recipe(name).unsafePerformIO.fold(
      (e) => Error("No recipe named %s installed.".format(name)).fail,
      (s) => {
        storage.listFiles
               .filter( f => f.isDirectory && f.getName == name )
               .headOption
               .foreach( recursiveDelete )
        "Removed %s from the storage".format(name).success
      })    
  }
  
  /*
   *
   * Related to fetching template files of a specific recipe. 
   * 
   */
  
  def template(recipe: String, template: String): IO[Option[File]] = io {
    file(storage.getAbsolutePath + / + recipe).listFiles.filter( _.getName == template).headOption
  }   
   
  /*
   *
   * Misc. helper functions. 
   * 
   */
  
  private val / = File.separator
  
  private def recursiveDelete(file: File): Unit = {
    if (file.isDirectory && !file.listFiles.isEmpty) {
      file.listFiles.foreach( recursiveDelete ) 
      file.delete
    } else {
      file.delete
    }
  }
  
  private def storeSourcesOfDescription(recipeName: String, description: Description): List[File] = {     
    // TODO: should return an option. 
    description.sources.flatMap { source => 
      download(new URL(source.url), file(List(storage.getAbsolutePath, recipeName, source.name).mkString(/)))
        .unsafePerformIO
        .fold(
          err => { println(err); None },
          succ => Some(succ)
        )
    }
  }
}