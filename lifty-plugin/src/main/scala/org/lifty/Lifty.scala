package org.lifty

import org.lifty.engine._
import org.lifty.engine.io.{ Storage }

import scalaz.{ Validation }
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
  
  /**
   *  The parsers. Makes sure that the input is well formed and provides very
   *  awesome tab-completion. 
   */
  object LiftyParsers {

    def listTuple(t: (String,String)): List[String] = List(t._1,t._2)

    // data
    def keywords = List("create", "templates", "learn", "delete", "upgrade", "recipes", "help")
    def recipes: List[String] = Storage.allRecipes.unsafePerformIO map (_.name)
    def templates: Map[String,Validation[Error,List[String]]] = 
      (recipes map ( r => (r,Storage.templateNames(r).unsafePerformIO))).toMap

    // parsers
    lazy val recipe: Parser[String] = token(NotSpace.examples(recipes : _ *))

    def template(recipe: String): Parser[(String,String)] = {
      if (templates.contains(recipe)) {
        templates(recipe).fold(
          (e) => failure(e.message + "\nplease reinstall the recipe"),
          (s) => token(NotSpace.examples(s : _ *).map( t => (recipe,t) ))
        )
      } else {
        failure("Lifty doesn't know any recipe named " + recipe)
      }
    }

    lazy val name: Parser[String] = NotSpace

    lazy val url: String => Parser[(String,String)] = (n) => NotSpace map ( u => (n,u) )

    lazy val createParser   : Parser[(String,String)] = recipe flatMap ( t => Space ~> template(t) )
    lazy val templatesParser: Parser[String]          = recipe
    lazy val learnParser    : Parser[(String,String)] = name flatMap ( t => Space ~> url(t) )
    lazy val deleteParser   : Parser[String]          = recipe
    lazy val upgradeParser  : Parser[String]          = recipe

    lazy val liftyParser: Parser[(String,List[String])] = {
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
