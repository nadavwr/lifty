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
  val XLifty    = config("xlifty")
  val create    = InputKey[Unit]("create")
  val templates = InputKey[Unit]("templates")
  val learn     = InputKey[Unit]("learn")
  val delete    = InputKey[Unit]("delete")
  val upgrade   = InputKey[Unit]("upgrade")
  val recipes   = TaskKey[Unit]("recipes")
  val help      = TaskKey[Unit]("help")

  val xliftySettings: Seq[sbt.Project.Setting[_]] = {
    inConfig(XLifty)(Seq(
      create    <<= InputTask(createParser)(createDef),
      templates <<= InputTask(templatesParser)(templatesDef),
      learn     <<= InputTask(learnParser)(learnDef),
      delete    <<= InputTask(deleteParser)(deleteDef),
      upgrade   <<= InputTask(upgradeParser)(upgradeDef),
      recipes   := recipesTask,
      help      := helpTask
    ))
  }


  // Task definitions

  private val recipesTask = recipes := {
    println("Would've listed recipes")
  }

  private val helpTask = help := {
    println("Would've printed help msg")
  }

  private val createDef = (parsedTask: TaskKey[(String,String)]) => {
    (parsedTask) map { case ( (recipe: String, template: String) ) =>
      println("Would've created " + template + " of " + recipe)
    }
  }

  private val templatesDef = (parsedTask: TaskKey[String]) => {
    (parsedTask) map { case (recipe: String) =>
      println("Templates of " + recipe)
    }
  }

  private val learnDef = (parsedTask: TaskKey[(String,String)]) => {
    (parsedTask) map { case (name: String, url: String) =>
      println("Would've learned " + name + " as " + url)
    }
  }
  private val deleteDef = (parsedTask: TaskKey[String]) => {
    (parsedTask) map { case (recipe: String) =>
      println("Would've deleted " + recipe)
    }
  }
  private val upgradeDef = (parsedTask: TaskKey[String]) => {
    (parsedTask) map { case (recipe: String) =>
      println("Would've updated " + name)
    }
  }
  
  object LiftyParsers {

    // List of available parsers:
    // http://harrah.github.com/xsbt/latest/api/sbt/complete/DefaultParsers$.html

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

    // I want to get rid of this as I don't need access to the State or scalaVersion
    def init[T](p: Parser[T]) = (scalaVersion) { s => (state: State) => p}

    val createParser: Initialize[State => Parser[(String,String)]] = init(recipe flatMap template)
    val templatesParser: Initialize[State => Parser[String]] = init(recipe)
    val learnParser: Initialize[State => Parser[(String,String)]] = init(name flatMap url)
    val deleteParser: Initialize[State => Parser[String]] = init(recipe)
    val upgradeParser: Initialize[State => Parser[String]] = init(recipe)


  }
  
}
