package org.lifty.engine.io

import org.lifty.engine.{ Description, Error }
import scalaz._
import scalaz.effects._
import Scalaz._
import java.io.{ FileInputStream, InputStreamReader, File}
import java.net.{ URL }
import net.liftweb.json.{ JsonParser, DefaultFormats }

/** 
 * Contains method(s) for loading and parsing the description.json files of 
 * recipes. 
 */
object DescriptionLoader {

  // Simply so it knows how to println this stuff for debugging.
  implicit val formats = DefaultFormats

  def load(description: URL): IO[Validation[Error, Description]] = io {
    val file = File.createTempFile("descriptor",".json")
    file.deleteOnExit() 
    Downloader.download(description, file).unsafePerformIO.flatMap { _ =>
      load(file).unsafePerformIO
    }
  }

  // Load a json description of a recipe 
  def load(description: File): IO[Validation[Error, Description]] = 
    FileUtil.readToString(description).map { strOpt => 
      strOpt.map { str: String => 
        try {
          val jvalue = JsonParser.parse(str)
          jvalue.extractOpt[Description]
                .map(_.success)
                .getOrElse(Error("Wasn't able to extract JSON to AST").fail)
        } catch {
          case e: Exception => 
            Error("Wasn't able to pase file %s, got stacktrace".format(description,e.getStackTrace)).fail
        }
      }.getOrElse(Error("Wasn't able to read file %s".format(description)).fail)
    }
}