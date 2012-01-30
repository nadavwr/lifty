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
  
  val liftyTask = InputKey[Unit]("lifty") 
  val liftyFolderSetting = SettingKey[String]("target-folder-lifty") /* Configure lifty to use another folder
                                                                        than '.' for the root of the generated
                                                                        files. (useful for tests, so I can clean up)*/
  val liftySettings = Seq(
    liftyFolderSetting := ".",
    liftyTask <<= InputTask(_ => liftyParser)(liftyDef)
  )
  
  type parsedResult = (String, List[String])
  
  lazy val liftyDef = 
    (parsedTask: TaskKey[parsedResult]) => {
      (parsedTask, liftyFolderSetting) map { (parsed, folder) => 
        (parsed match {
          case ("create", x :: y :: Nil) => LiftyEngine.create(x,y, LiftyConfiguration(folder)) 
          case ("templates", x :: Nil)   => LiftyEngine.templates(x)
          case ("learn", x :: y :: Nil)  => LiftyEngine.learn(x,y)
          case ("delete", x :: Nil)      => LiftyEngine.delete(x)
          case ("update", x :: Nil)     => LiftyEngine.upgrade(x)
          case ("recipes", Nil)          => LiftyEngine.recipes()
          case ("help", Nil)             => LiftyEngine.help()
        }).fold(
          e => { println("\n" + e.message + "\n") },
          s => { println("\n"+s+"\n") }
        )
      }
    }    
  
  /**
   *  The parsers. Makes sure that the input is well formed and provides very
   *  awesome tab-completion. 
   */
  object LiftyParsers {

    def listTuple(t: (String,String)): List[String] = List(t._1,t._2)

    // data
    def keywords = List("create", "templates", "learn", "delete", "update", "recipes", "help")
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
        case "update"   => Space ~> upgradeParser map { p => (cmd, List(p)) }
        case _   => success { (cmd, Nil) }
      }}
    }
  }
}
