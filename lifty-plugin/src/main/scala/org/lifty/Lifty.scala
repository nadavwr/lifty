package org.lifty

import java.net.{ URL }
import java.io.{ File }
import org.lifty.engine._
import org.lifty.engine.io.{ Storage }

import scalaz.effects.IO

import sbt._
import Keys._
import Defaults._
import complete.DefaultParsers._
import complete.{ Parser }

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

    def listTuple(t: (String,String)): List[String] = List(t._1,t._2)

    // data

    val keywords = List("create", "templates", "learn", "delete", "upgrade", "recipes", "help")
    val recipes: List[String] = Storage.allRecipes.unsafePerformIO map (_.name)
    val templates: Map[String,List[String]] = (recipes map ( r => (r,Storage.templateNames(r).unsafePerformIO.toOption.get))).toMap

    // parsers

    val recipe: Parser[String] = token(NotSpace.examples(recipes : _ *) <~ Space)

    val template: String => Parser[(String,String)] = (r) => token(NotSpace.examples(templates(r) : _ *) map ( t => (r,t) ))

    val name: Parser[String] = Space ~> NotSpace <~ Space

    val url: String => Parser[(String,String)] = (n) => NotSpace map ( u => (n,u) )

    val createParser   : Parser[(String,String)] = recipe flatMap template
    val templatesParser: Parser[String]          = recipe
    val learnParser    : Parser[(String,String)] = name flatMap url
    val deleteParser   : Parser[String]          = recipe
    val upgradeParser  : Parser[String]          = recipe

    val liftyParser: Parser[(String,List[String])] = {
      Space ~>
      token(NotSpace.examples(keywords : _ *) <~ Space) flatMap { cmd => cmd match {
        case "create"    => createParser map { p => (cmd,listTuple(p)) }
        case "templates" => templatesParser map { p => (cmd, List(p)) }
        case "learn"     => learnParser map { p => (cmd, listTuple(p)) }
        case "delete"    => deleteParser map { p => (cmd, List(p)) }
        case "upgrade"   => upgradeParser map { p => (cmd, List(p)) }
        case _           => success { (cmd,List[String]()) }
      }}
    }

  }

}
