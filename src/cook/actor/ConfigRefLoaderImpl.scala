package cook.actor

import cook.actor.util.BatchResponser
import cook.app.Global
import cook.config.ConfigRef
import cook.ref.FileRef

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration._
import scala.collection.mutable
import scala.util.{ Try, Success, Failure }
import akka.actor.TypedActor

object ConfigRefLoadTask {

  def apply(refName: String)(runBlock: => Unit): Runnable = new Runnable {
    override def run() {
      runBlock
    }
  }
}

class ConfigRefLoaderImpl extends ConfigRefLoader {

  private val cache = mutable.Map[String, ConfigRef]()
  private val responser = new BatchResponser[String, ConfigRef]()

  override def taskComplete(refName: String)(tryConfigRef: Try[ConfigRef]) {
    tryConfigRef match {
      case Success(configRef) =>
        cache(refName) = configRef
        responser.success(refName, configRef)
      case Failure(e) =>
        responser.failure(refName, e)
    }
  }

  override def loadConfigRef(cookFileRef: FileRef): Future[ConfigRef] = {
    val refName = cookFileRef.refName
    cache.get(refName) match {
      case Some(configRef) =>
        Future.successful(configRef)
      case None =>
        responser.onTask(refName) {
          doLoadConfigRef(refName, cookFileRef)
        }
    }
  }

  private def doLoadConfigRef(refName: String, cookFileRef: FileRef) {
    val self = TypedActor.self[ConfigRefLoader]
    Global.workerDispatcher.execute(ConfigRefLoadTask(refName) {
      self.taskComplete(refName)(Try(new ConfigRef(cookFileRef)))
    })
  }
}
