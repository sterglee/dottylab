
package rsyntaxEdit

import scalaExec.Interpreter.GlobalValues

// processes a word over which we double click with the mouse
// this will generally display information about the variable

object ProcessDoubleClick {

  // process the double click over the editor's text word wd
  def processDoubleClick(wd: String) = {
    var wordAtCursor = wd

    var valueOfId = GlobalValues.scala3Engine.eval(wordAtCursor)

    println(wordAtCursor + " = " + valueOfId)

  }

}
