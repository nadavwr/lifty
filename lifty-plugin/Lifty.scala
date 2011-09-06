package org.lifty

import java.net.{ URL }
import org.lifty.engine._
import sbt._
import Keys._
import xsbti.{Logger}

object Lifty extends Plugin {

  override lazy val settings = Seq(commands += liftyCommand)

  lazy val liftyCommand =
    Command.args("lifty","<help>") { (state, args) =>
      LiftyInstance.run(args.toList).fold(
        e => {
          println("\n" + e.message + "\n")
          state.fail
        },
        s => {
          println("\n"+s+"\n")
          state
        }
      )
    }

}
