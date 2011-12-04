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

  override lazy val settings = Seq(commands += liftyCommand)

  val liftyCommand = Command("lifty")(_ => liftyParser) {
    (state: State, p: (String,List[String])) => {
      liftyDef(p)
      state
    }
  }

  val liftyDef = (p: (String, List[String])) => { p match {
    case (cmd: String, args: List[String]) => {
      cmd match {
        case "create"    => println(cmd + args)
        case "templates" => println(cmd + args)
        case "learn"     => println(cmd + args)
        case "delete"    => println(cmd + args)
        case "upgrade"   => println(cmd + args)
        case "recipes"   => println(cmd + args)
        case "help"      => println(cmd + args)
        case _ => println("unsupported")
      }
    }
  }}

  object LiftyParsers {

    val keywords = List("create", "templates", "learn", "delete", "upgrade", "recipes", "help")

    def listTuple(t: (String,String)): List[String] = List(t._1,t._2)

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

    val liftyParser: Parser[(String,List[String])] = {
      Space ~>
      NotSpace.examples(keywords : _ *) ~ (
        ( Space ~>
          (createParser map (listTuple _)) |
          (templatesParser map (List(_)))  |
          (learnParser map (listTuple _))  |
          (deleteParser map (List(_)))     |
          (upgradeParser map (List(_)))
        ).??( List[String]() ))
    }

  }

}
