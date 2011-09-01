package org.lifty.engine

import java.io.{ File }

// http://stevegilham.blogspot.com/2009/01/pipe-operator-in-scala.html
object Functional {
    class PipedObject[T] private[Functional] (value:T)
    {
        def |>[R] (f : T => R) = f(this.value)
    }
    implicit def toPiped[T] (value:T) = new PipedObject[T](value)
}

object Util {
  
  def properPath(path: String) = {
    File.separator match {
      case "/"  => path
     	case "\\" => path.replace("/","""\\""") 
    }
  }
  
  def packageAsPath(p: String) = p.replace(".",File.separator )
  
}