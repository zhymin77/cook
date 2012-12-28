package cook.target

import cook.error.ErrorTracking._

import scala.tools.nsc.io.Path


trait TargetMeta { self: Target =>

  /**
   * Check if meta changed.
   *
   * Target need iff meta changed.
   *
   * 1. check internal meta
   *    - dep targets meta
   *    - cook context meta
   * 2. check external meta
   *    - generated by inputMetaFn
   */
  private def checkIfMetaChanged: Boolean = {

    internalMeta

    // TODO(timgreen):
    true
  }

  protected lazy val internalMeta: Target.InputMeta = {



  }
}
