package cook.target


class Target[T](
  ref: TargetRef,
  buildCmd: Target.BuildCmd[T],
  resultFn: Target.ResultFn[T],
  inputMetaFn: Target.InputMetaFn[T],
  runCmd: Option[Target.RunCmd[T]],
  deps: List[TargetRef]
) {

  private var _result: Option[T] = None
  private var built: Boolean = false
  private lazy val inputMeta = inputMetaFn(this)
  private lazy val isMetaNotChanged = checkIfMetaChanged(inputMeta)

  def result: T = {
    // TODO(timgreen):
    // only avaliable in build stage
    assert(!needBuild, "when assign target.result, target must be built")
    _result match {
      case Some(r) => r
      case None =>
        val r = resultFn(this)
        _result = Some(r)
        r
    }
  }

  def needBuild = !built && isMetaNotChanged

  def build {
    if (needBuild) {
      buildCmd(this)
      built = true
    }
  }

  private def checkIfMetaChanged(inputMeta: TargetInputMeta): Boolean = {
    // TODO(timgreen):
    true
  }
}

object Target {

  type BuildCmd[T] = Target[T] => Unit
  type ResultFn[T] = Target[T] => T
  type InputMetaFn[T] = Target[T] => TargetInputMeta
  type RunCmd[T] = Target[T] => Int

  def apply[T](
    ref: TargetRef,
    buildCmd: BuildCmd[T],
    resultFn: ResultFn[T],
    inputMetaFn: InputMetaFn[T],
    runCmd: Option[RunCmd[T]] = None,
    deps: List[TargetRef] = List()
  ) = {
    new Target(ref, buildCmd, resultFn, inputMetaFn, runCmd, deps)
  }
}
