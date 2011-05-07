package cook.config.runner.value

import cook.config.parser.unit._
import cook.config.runner.EvalException

abstract class Value(val typeName: String) {

  def attr(id: String): Value = id match {
    case "tos" => StringValue(this.toString)
    case _ => throw new EvalException("Unsupportted attr \"%s\" on %s", id, typeName)
  }
  def unaryOp(op: String): Value = {
    throw new EvalException("Unsupportted UnaryOperation \"%s\" on %s", op, typeName)
  }

  def isNull = false
  def get(): Any

  def toString(): String

  //
  def tos(): String = this match {
    case StringValue(str) => str
    case _ => {
      // TODO(timgreen): better error message
      throw new EvalException("Need StringValue here")
    }
  }
}

case class NullValue() extends Value("Null") {

  override def isNull = true
  override def get(): Any = null
  override def toString(): String = "null"
}

case class BooleanValue(bool: Boolean) extends Value("Bool") {

  override def unaryOp(op: String): Value = op match {
    case "!" => BooleanValue(!bool)
    case _ => super.unaryOp(op)
  }

  override def get(): Any = bool
  override def toString(): String = bool.toString
}
object BooleanValue {

  val TRUE  = BooleanValue(true)
  val FALSE = BooleanValue(false)
}

case class NumberValue(int: Int) extends Value("Number") {

  override def get(): Any = int
  override def toString(): String = int.toString
}

case class StringValue(str: String) extends Value("String") {

  override def attr(id: String): Value = id match {
    case "size" => NumberValue(str.size)
    case "length" => NumberValue(str.length)
    case "isEmpty" => BooleanValue(str.isEmpty)
    case "nonEmpty" => BooleanValue(str.nonEmpty)
    case _ => super.attr(id)
  }

  override def get(): Any = str
  override def toString(): String = str
}

case class CharValue(c: Char) extends Value("Char") {

  override def get(): Any = c
  override def toString(): String = c.toString
}

case class ListValue(list: Seq[Value]) extends Value("List") {

  override def attr(id: String): Value = id match {
    case "size" => NumberValue(list.size)
    case "length" => NumberValue(list.length)
    case "isEmpty" => BooleanValue(list.isEmpty)
    case "nonEmpty" => BooleanValue(list.nonEmpty)
    case _ => super.attr(id)
  }

  override def get(): Any = list
  override def toString(): String = list.mkString("[", ", ", "]")
}

object ListValue {

  def apply(): ListValue = ListValue(Seq[Value]())
}
