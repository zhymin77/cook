package cook.config.parser.unit

class CookConfig(val statements: Array[Statement])

abstract class Statement

abstract class FuncStatement extends Statement
case class Assginment(id: String, expr: Expr) extends FuncStatement

case class ExprItem(val simpleExprItem: SimpleExprItem, val selectorSuffixs: Array[SelectorSuffix])

abstract class SimpleExprItem extends FuncStatement
case class IntegerConstant(int: Int) extends SimpleExprItem
case class StringLiteral(str: String) extends SimpleExprItem
case class Identifier(id: String) extends SimpleExprItem
case class FuncCall(name: String, args: Array[Arg]) extends SimpleExprItem
case class ExprList(exprs: Array[Expr]) extends SimpleExprItem
case class Expr(exprItems: Array[ExprItem], ops: Array[String]) extends SimpleExprItem

abstract class Arg
case class ArgValue(expr: Expr) extends Arg
case class ArgNamedValue(name: String, expr: Expr) extends Arg

abstract class SelectorSuffix
case class IdSuffix(id: String) extends SelectorSuffix
case class CallSuffix(call: FuncCall) extends SelectorSuffix

case class FuncDef(name: String,
                   args: Array[Arg],
                   statements: Array[FuncStatement],
                   returnStatement: ReturnStatement) extends Statement

case class ReturnStatement(val expr: Expr)