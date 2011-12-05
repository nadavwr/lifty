package org.lifty

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

  val liftyCommand = Command("lifty")(_ => liftyParser)(liftyDef _)

  def liftyDef(state: State, p: (String, List[String])) = { 
    val (cmd: String, args: List[String]) = p
    LiftyEngine.runCommand(cmd, args).fold(
      e => { println("\n" + e.message + "\n") ; state.fail },
      s => { println("\n"+s+"\n") ; state }
    )
  }

  object LiftyParsers {

    def listTuple(t: (String,String)): List[String] = List(t._1,t._2)

    // data

    val keywords = List("create", "templates", "learn", "delete", "upgrade", "recipes", "help")
    val recipes: List[String] = Storage.allRecipes.unsafePerformIO map (_.name)
    val templates: Map[String,List[String]] = (recipes map ( r => (r,Storage.templateNames(r).unsafePerformIO.toOption.get))).toMap

    // parsers

    val recipe: Parser[String] = token(NotSpace.examples(recipes : _ *))

    def template(recipe: String): Parser[(String,String)] = {
      if (templates.contains(recipe)) {
        token(NotSpace.examples(templates(recipe) : _ *).map( t => (recipe,t) ))
      } else {
        failure(recipe + "Lifty doesn't know any recipe named " + recipe)
      }
    }

    val name: Parser[String] = NotSpace

    val url: String => Parser[(String,String)] = (n) => NotSpace map ( u => (n,u) )

    val createParser   : Parser[(String,String)] = recipe flatMap ( t => Space ~> template(t) )
    val templatesParser: Parser[String]          = recipe
    val learnParser    : Parser[(String,String)] = name flatMap ( t => Space ~> url(t) )
    val deleteParser   : Parser[String]          = recipe
    val upgradeParser  : Parser[String]          = recipe

    val liftyParser: Parser[(String,List[String])] = {
      Space ~>
      token(NotSpace.examples(keywords : _ *)) flatMap { cmd => cmd match {
        case "create"    => Space ~> createParser map { p => (cmd,listTuple(p)) }
        case "templates" => Space ~> templatesParser map { p => (cmd, List(p)) }
        case "learn"     => Space ~> learnParser map { p => (cmd, listTuple(p)) }
        case "delete"    => Space ~> deleteParser map { p => (cmd, List(p)) }
        case "upgrade"   => Space ~> upgradeParser map { p => (cmd, List(p)) }
        case _           => success { (cmd,List[String]()) }
      }}
    }
  }
}
