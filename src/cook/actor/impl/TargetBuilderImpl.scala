package cook.actor.impl

import cook.actor.TargetBuilder
import cook.actor.impl.util.BatchResponser
import cook.actor.impl.util.TaskBuilder
import cook.app.Global
import cook.ref.TargetRef
import cook.target.Target
import cook.target.TargetResult
import cook.util.DagSolver

import akka.actor.TypedActor
import scala.collection.mutable
import scala.concurrent.{ Promise, Future, Await }
import scala.util.{ Try, Success, Failure }


object TargetBuildTask extends TaskBuilder("TargetBuild")


/**
 * step1: get target
 * step2: wait all dep target
 * step3: bulid self target
 */
class TargetBuilderImpl extends TargetBuilder with TypedActorBase {

  import ActorRefs._

  private val responser = new BatchResponser[String, TargetResult]()
  private val dagSolver = new DagSolver
  private val pendingTargets = mutable.Map[String, Target[TargetResult]]()
  private def self = targetBuilder

  override def taskComplete(refName: String)(tryTargetResult: Try[TargetResult]) {
    responser.complete(refName)(tryTargetResult)
    if (tryTargetResult.isSuccess) {
      dagSolver.markDone(refName)
      self.checkDag
    }
  }

  override def build(targetRef: TargetRef): Future[TargetResult] = {
    val refName = targetRef.refName
    log.debug("building {}", refName)
    responser.onTask(refName) {
      step1GetTarget(targetRef)
    }
  }

  private def step1GetTarget(targetRef: TargetRef) {
    import TypedActor.dispatcher
    targetManager.getTarget(targetRef) onComplete {
      case Success(t) =>
        self.step2WaitForDeps(t)
      case Failure(e) =>
        log.debug("step1GetTarget.failure {}", e)
        self.taskComplete(targetRef.refName)(Failure(e))
    }
  }

  override def step2WaitForDeps(target: Target[TargetResult]) {
    pendingTargets(target.refName) = target
    dagSolver.addDeps(target.refName, target.deps.map(_.refName))
    import TypedActor.dispatcher
    Future.sequence(target.deps map self.build) onFailure {
      case e =>
        self.taskComplete(target.refName)(Failure(e))
    }
    self.checkDag
  }

  override def checkDag {
    if (dagSolver.hasAvaliable) {
      val avaliableTargetName = dagSolver.pop
      self.step3BuildTarget(avaliableTargetName)
      self.checkDag
    }
  }

  override def step3BuildTarget(targetName: String) {
    val optTarget = pendingTargets.remove(targetName)
    assert(optTarget.isDefined, "the target ready for build must in pending list: " + targetName)
    val target = optTarget.get

    Global.workerDispatcher.execute(TargetBuildTask(targetName) {
      self.taskComplete(targetName)(Try {
        target.build
        target.result
      })
    })
  }
}
