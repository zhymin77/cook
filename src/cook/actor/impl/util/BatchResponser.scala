package cook.actor.impl.util

import scala.collection.mutable
import scala.concurrent.{ Promise, Future }
import scala.util.{ Try, Success, Failure }

/** Batch message responser with cache.
  */
class BatchResponser[Key, Result](processError: (Key, Throwable) => Throwable) {

  private val waiters = mutable.Map[Key, Promise[Result]]()
  private val cache = mutable.Map[Key, Result]()
  private val cacheError = mutable.Map[Key, Throwable]()

  def onTask(key: Key)(firstTimeAction: => Unit): Future[Result] = cache.get(key) match {
    case Some(result) =>
      Future.successful(result)
    case None =>
      cacheError.get(key) match {
        case Some(e) =>
          Future.failed(e)
        case None =>
          waiters.getOrElseUpdate(key, {
            val p = Promise[Result]()
            firstTimeAction
            p
          }).future
      }
  }

  def success(key: Key, result: Result) {
    waiters.remove(key) match {
      case None =>
        assert(false, "Can not complete an unexist task as success: " + key)
      case Some(p) =>
        cache(key) = result
        p.success(result)
    }
  }

  def failure(key: Key, e: Throwable) {
    waiters.remove(key) match {
      case None =>
        assert(false, "Can not complete an unexist task as failure: " + key)
      case Some(p) =>
        val error = processError(key, e)
        cacheError(key) = error
        p.failure(error)
    }
  }

  def complete(key: Key)(tryResult: Try[Result]) {
    tryResult match {
      case Success(result) => success(key, result)
      case Failure(e) => failure(key, e)
    }
  }
}
