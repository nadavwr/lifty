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
import org.lifty.engine.{ Error, Description }
import Downloader.{ download }
import DescriptionLoader.{ load }
import org.lifty.engine.io.FileUtil.{ file }

case class Recipe(descriptor: File, templates: Seq[File])

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
trait Storage {

  val root: File

  lazy val storage = file(root.getAbsolutePath + / + ".lifty") // TODO: Create it if it doesn't exist

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
      Recipe(descriptor, templates).success
    }).getOrElse(Error("No recipe named %s in the storage.".format(name)).fail)
  }

  /** 
   * Attempts to store a recipe at the given url in the storage under the given name. 
   * 
   * @param name The name of the recipe
   * @param url The URL of the recipe's .json description file
   */
  def storeRecipe(name: String, url: URL): IO[Validation[Error, Recipe]] = {
    
    val recipe = file(List(storage.getAbsolutePath, name, name+".json").mkString(/))

    download(url, recipe).map( _.fold( err => err.fail,
      file => load(file).unsafePerformIO.fold( err => err.fail,
        description => Recipe(file, storeSourcesOfDescription(name, description)).success
      )
    )) 
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
   * Deletes a recipe from the store 
   * 
   * @param name The name of the recipe
   */
  def deleteRecipe(name: String): IO[String] = io {
    storage.listFiles
           .filter( f => f.isDirectory && f.getName == name )
           .headOption
           .foreach( recursiveDelete )
    "Removed %s from the storage".format(name)
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

object HomeStorage extends Storage {

  val root = file(System.getProperty("user.home"))

}