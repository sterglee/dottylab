package scala.tools.nsc.interpreter

import scalaExec.Interpreter.GlobalValues

import java.util
import javax.script.ScriptEngineManager

class IMain {
  def interpret(script: String) = {


    System.out.println("evaluating "+script)
  //  GlobalValues.scala3Engine.eval("println(\"Scala3\")")
//GlobalValues.replDriver.run(script)( GlobalValues.replDriver.initialState)
   GlobalValues.scala3Engine.eval(script)
  }
  def typeOfTerm(expr: String) = "??"
  def valueOfTerm(expr: String) = "???"

}

object IMain {

}
