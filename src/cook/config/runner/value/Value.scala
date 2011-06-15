package cook.config.runner.value

import scala.collection.mutable.HashMap

import cook.config.parser.unit._
import cook.config.runner.EvalException
import cook.util._

abstract class Value(val typeName: String) {

  def attr(id: String): Value = id match {
    case "tos" => StringValue(this.toString)
    case "isNull" => BooleanValue.FALSE
    case "isChar" => BooleanValue.FALSE
    case "isBool" => BooleanValue.FALSE
    case "isStr" => BooleanValue.FALSE
    case "isInt" => BooleanValue.FALSE
    case "isList" => BooleanValue.FALSE
    case "isLabel" => BooleanValue.FALSE
    case "isFileLabel" => BooleanValue.FALSE
    case "isTargetLabel" => BooleanValue.FALSE
    case "isFunction" => BooleanValue.FALSE
    case _ => throw new EvalException("Unsupportted attr \"%s\" on %s", id, typeName)
  }
  def unaryOp(op: String): Value = {
    throw new EvalException("Unsupportted UnaryOperation \"%s\" on %s", op, typeName)
  }

  def isNull = false
  def get(): Any

  def toString(): String

  // Type convert
  def toChar: Char = toChar("Need CharValue here")
  def toChar(errorMessage: String) = this match {
    case CharValue(c) => c
    case _ => throw new EvalException(errorMessage)
  }

  def toStr: String = toStr("Need StringValue here")
  def toStr(errorMessage: String) = this match {
    case StringValue(str) => str
    case _ => throw new EvalException(errorMessage)
  }

  def toInt: Int = toInt("Need NumberValue here")
  def toInt(errorMessage: String) = this match {
    case NumberValue(int) => int
    case _ => throw new EvalException(errorMessage)
  }

  def toBool: Boolean = toBool("Need BooleanValue here")
  def toBool(errorMessage: String) = this match {
    case BooleanValue(bool) => bool
    case _ => throw new EvalException(errorMessage)
  }

  def toTargetLabel: TargetLabel = toTargetLabel("Need TargetLabel here")
  def toTargetLabel(errorMessage: String) = this match {
    case TargetLabelValue(targetLabel) => targetLabel
    case _ => throw new EvalException(errorMessage)
  }

  def toFileLabel: FileLabel = toFileLabel("Need FileLabel here")
  def toFileLabel(errorMessage: String) = this match {
    case FileLabelValue(fileLabel) => fileLabel
    case _ => throw new EvalException(errorMessage)
  }

  def toListValue(errorMessage: String): Seq[Value] = this match {
    case ListValue(list) => list
    case _ => throw new EvalException(errorMessage)
  }

  def toListStr: Seq[String] = toListStr("Need List StringValue here")
  def toListStr(errorMessage: String) = {
    this.toListValue(errorMessage).map { _.toStr(errorMessage) }
  }

  def toListChar: Seq[Char] = toListChar("Need List CharValue here")
  def toListChar(errorMessage: String) = {
    this.toListValue(errorMessage).map { _.toChar(errorMessage) }
  }

  def toListTargetLabel: Seq[TargetLabel] = toListTargetLabel("Need List TargetLabel here")
  def toListTargetLabel(errorMessage: String) = {
    this.toListValue(errorMessage).map { _.toTargetLabel(errorMessage) }
  }

  def toListFileLabel: Seq[FileLabel] = toListFileLabel("Need List FileLabel here")
  def toListFileLabel(errorMessage: String) = {
    this.toListValue(errorMessage).map { _.toFileLabel(errorMessage) }
  }
}

case class NullValue() extends Value("Null") {

  override def isNull = true
  override def get(): Any = null
  override def toString(): String = "null"
  override def attr(id: String): Value = id match {
    case "isNull" => BooleanValue.TRUE
    case _ => super.attr(id)
  }
}

case class BooleanValue(bool: Boolean) extends Value("Bool") {

  override def unaryOp(op: String): Value = op match {
    case "!" => BooleanValue(!bool)
    case _ => super.unaryOp(op)
  }
  override def attr(id: String): Value = id match {
    case "isBool" => BooleanValue.TRUE
    case _ => super.attr(id)
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
  override def attr(id: String): Value = id match {
    case "isInt" => BooleanValue.TRUE
    case _ => super.attr(id)
  }
  override def toString(): String = int.toString
}

case class StringValue(str: String) extends Value("String") {

  override def attr(id: String): Value = id match {
    case "isStr" => BooleanValue.TRUE
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
  override def attr(id: String): Value = id match {
    case "isChar" => BooleanValue.TRUE
    case _ => super.attr(id)
  }
  override def toString(): String = c.toString
}

case class ListValue(list: Seq[Value]) extends Value("List") {

  override def attr(id: String): Value = id match {
    case "isList" => BooleanValue.TRUE
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

abstract class LabelValue(typeName: String) extends Value(typeName)

object LabelValue {

  def apply(label: Label): LabelValue = label match {
    case fileLabel: FileLabel => FileLabelValue(fileLabel)
    case targetLabel: TargetLabel => TargetLabelValue(targetLabel)
  }
}

case class FileLabelValue(fileLabel: FileLabel) extends LabelValue("FileLabel") {

  override def get(): Any = fileLabel
  override def attr(id: String): Value = id match {
    case "isLabel" => BooleanValue.TRUE
    case "isFileLabel" => BooleanValue.TRUE
    case "file" => StringValue(fileLabel.file.getAbsolutePath)
    case _ => super.attr(id)
  }
  override def toString(): String = "<FileLabel:" + fileLabel.file.getAbsolutePath + ">"
}

object FileLabelValue {

  def apply(absPath: String): FileLabelValue = {
    FileLabelValue(new FileLabel(null, absPath))
  }
}

case class TargetLabelValue(targetLabel: TargetLabel) extends LabelValue("TargetLabel") {

  override def get(): Any = targetLabel
  override def attr(id: String): Value = id match {
    case "isLabel" => BooleanValue.TRUE
    case "isTargetLabel" => BooleanValue.TRUE
    case "outputDir" => StringValue(targetLabel.outputDir.getAbsolutePath)
    case _ => super.attr(id)
  }
  override def toString(): String = "<TargetLabel:" + targetLabel.targetName + ">"
}

class ArgsDef(val names: Seq[String], val defaultValues: HashMap[String, Value])
class FunctionValue(val path: String,
                    val scope: Scope,
                    val argsDef: ArgsDef,
                    val statements: Seq[FuncStatement],
                    val returnStatement: Option[Expr]) extends Value("Function") {

  override def get(): Any = {
    throw new UnsupportedOperationException("FunctionValue doesn't have wrapped data")
  }
  override def attr(id: String): Value = id match {
    case "isFunction" => BooleanValue.TRUE
    case _ => super.attr(id)
  }
  override def toString(): String = "<FunctionValue>"
}
abstract class BuildinFunction(argsDef: ArgsDef)
    extends FunctionValue(null, null, argsDef, null, null) {

  def eval(path: String, argsValue: Scope): Value
  override def toString(): String = "<BuildinFunction>"
}
