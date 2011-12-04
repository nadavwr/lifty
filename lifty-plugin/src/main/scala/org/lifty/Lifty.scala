package org.lifty

import java.net.{ URL }
import java.io.{ File }
import org.lifty.engine._

import sbt._
import Keys._
import Defaults._
import Project.Initialize
import complete.DefaultParsers._
import complete.{ Parser }
import sbt.compiler.{ RawCompiler }
import xsbti.{Logger}

import org.fusesource.scalate.{ TemplateEngine }
import org.fusesource.scalate.support.{ Compiler }

object Lifty extends Plugin {

  import LiftyParsers._
  
  // Keys
  val Lifty     = config("lifty")
  val create    = InputKey[Unit]("create")
  val templates = InputKey[Unit]("templates")
  val learn     = InputKey[Unit]("learn")
  val delete    = InputKey[Unit]("delete")
  val upgrade   = InputKey[Unit]("upgrade")
  val recipes   = TaskKey[Unit]("recipes")
  val help      = TaskKey[Unit]("help")

  val liftySettings: Seq[sbt.Project.Setting[_]] = {
    inConfig(Lifty)(Seq(
      recipes   := recipesTask,
      help      := helpTask,
      create    := InputTask.static(createParser)(createDef),
      templates := InputTask.static(templatesParser)(templatesDef),
      learn     := InputTask.static(learnParser)(learnDef),
      delete    := InputTask.static(deleteParser)(deleteDef),
      upgrade   := InputTask.static(upgradeParser)(upgradeDef)
    ))
  }

  // TODO: Surely there's a better way than this. 
  def tsk(f: () => Unit) = {
    Task(
      Info(AttributeMap.empty, (x:Unit) => AttributeMap.empty),
      Pure { () => f() }
    )
  }

  def recipesTask {
    println("Would've listed recipes")
  }

  def helpTask = {
    println("Would've printed help msg")
  }

  val createDef = (p: (String,String)) => p match {
    case (recipe: String, template: String) =>
      tsk { () => println("eww") }
  }

  val templatesDef = (p: String) => p match {
    case (recipe: String) =>
      tsk { () => println("Templates of " + recipe) }
  }

  val learnDef = (p: (String,String)) => p match {
    case (name: String, url: String) =>
      tsk { () => println("Would've learned " + name + " as " + url) }
  }
  
  val deleteDef = (p: String) => p match {
    case (recipe: String) => 
      tsk { () => println("Would've deleted " + recipe) }
  }
  
  val upgradeDef = (p: String) => p match {
    case (recipe: String) =>
      tsk { () => println("Would've updated " + name) }
  }
  
  object LiftyParsers {

    // Sample data for now.
    val recipes = "lift" :: "sbt" :: Nil
    val templates = Map(
      "lift" -> List("project", "project-blank", "snippet", "layout"),
      "sbt"  -> List("plugin")
    )

    val recipe: Parser[String] = Space ~> NotSpace.examples(recipes : _ *) <~ Space

    val template: String => Parser[(String,String)] = (r) => NotSpace.examples(templates(r) : _ *) map ( t => (r,t) )

    val name: Parser[String] = Space ~> NotSpace <~ Space

    val url: String => Parser[(String,String)] = (n) => NotSpace map ( u => (n,u) )

    val createParser   : Parser[(String,String)] = recipe flatMap template
    val templatesParser: Parser[String]          = recipe
    val learnParser    : Parser[(String,String)] = name flatMap url
    val deleteParser   : Parser[String]          = recipe
    val upgradeParser  : Parser[String]          = recipe
  }
  
}
