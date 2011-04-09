package cook.parser.unit

abstract class Value
case class StringValue(value: String) extends Value
case class ListValue(value: Array[String]) extends Value

class Param(val key: String, val value: Value)

abstract class Command
case class BuildRule(val ruleName: String, val params: Map[String, Value]) extends Command

class BuildConfig(val path: String, val commands: Array[Command])
