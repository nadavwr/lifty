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
          println(e.message)
          state.fail
        },
        s => {
          println(s)
          state
        }
      )
    }

}
