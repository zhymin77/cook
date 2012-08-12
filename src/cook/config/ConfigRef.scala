package cook.config

import cook.error.ErrorTracking
import cook.path.PathRef
import cook.path.PathUtil

import java.util.concurrent.{ ConcurrentHashMap => JConcurrentHashMap }
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.tools.nsc.io.Path


object ConfigType extends Enumeration {
  type ConfigType = Value
  val CookRootConfig, CookConfig, CookiConfig = Value
}

case class ImportDefine(ref: ConfigRef, name: String, importMembers: Boolean)

private[config] class ConfigRef(segments: List[String])
  extends PathRef(segments) with ConfigMeta with ErrorTracking {

  private def verify {
    if (!p.canRead) {
      reportError("Can not read config: %s", p.path)
    }

    ConfigRef.checkCycleImport(this);

    if (configType == ConfigType.CookRootConfig) {
      Source.fromFile(p.path) getLines() forall { line =>
        (line matches ConfigRef.ImportP.toString) ||
          (line matches ConfigRef.ValP.toString) ||
          (line matches """^\s*//.*$""")
      } match {
        case true =>
        case false =>
          // NOTE(timgreen): only always imports in cook root config for now.
          reportError("Only always imports in COOK_ROOT config for now.")
      }
    }
  }

  lazy val configType = segments.last match {
    case "COOK_ROOT" => ConfigType.CookRootConfig
    case "COOK" => ConfigType.CookConfig
    case f if f.endsWith(".cooki") => ConfigType.CookiConfig
    case _ =>
      reportError("This should never happen, unknown config: %s", p.path)
  }

  val packagePrefix = "COOK_CONFIG_PACKAGE"
  lazy val configClassPackageName = (packagePrefix :: segments.dropRight(1)) mkString "."
  lazy val configClassFullName = configClassPackageName + "." + configClassName
  lazy val configClassName = configType match {
    case ConfigType.CookiConfig =>
      segments.last.replace(".", "_")
    case _ =>
      segments.last
  }
  lazy val configClassTraitName = configClassName + "Trait"
  lazy val configClassTraitFullName = configClassPackageName + "." + configClassTraitName

  lazy val configScalaSourceFile =
    PathUtil().cookConfigScalaSourceDir / (configClassFullName + ".scala")
  lazy val configByteCodeDir = PathUtil().cookConfigByteCodeDir / configClassFullName

  lazy val imports: Set[ImportDefine] = loadImports
  private def loadImports = {
    Source.fromFile(p.path) getLines() collect {
      case ConfigRef.ImportP(name, ref) =>
        ImportDefine(relativeConfigRef(ref + ".cooki"), name, true)
      case ConfigRef.ValP(name, ref) =>
        ImportDefine(relativeConfigRef(ref + ".cooki"), name, false)
    } toSet
  }

  private def relativeConfigRef(ref: String) = {
    ConfigRef(relativePathRefSegments(ref))
  }
}

object ConfigRef extends ErrorTracking {

  def apply(path: Path): ConfigRef = cache.get(path.path) match {
    case Some(c) => c
    case None =>
      val c = createConfigRef(path)
      cache(path.path) = c
      c.verify
      c
  }
  def apply(segments: List[String]): ConfigRef =
    apply(PathUtil().relativeToRoot(segments: _*))

  val ImportP = """\s*//\s*@import\s+(\w+)\s*=\s*"(.+)"\s*$""".r
  val ValP = """^\s*//\s*@val\s+(\w+)\s*=\s*"(.+)"\s*$""".r
  def rootConfigRef = apply(List("COOK_ROOT"))

  private def createConfigRef(path: Path) = wrapperError("Creating ConfigRef: %s", path.path) {
    new ConfigRef(PathUtil().relativeToRoot(path))
  }

  private [config] val cache: mutable.ConcurrentMap[String, ConfigRef] =
    new JConcurrentHashMap[String, ConfigRef]

  val cycleCheckPassed = mutable.Set[String]()
  private def checkCycleImport(ref: ConfigRef) {
    ref.configType match {
      case ConfigType.CookiConfig =>
        val trace = mutable.Set[String]()
        doCycleCheck(trace, ref)
        cycleCheckPassed += ref.p.path
      case _ => // pass
    }
  }

  private def doCycleCheck(trace: mutable.Set[String], ref: ConfigRef): Unit = {
    if (cycleCheckPassed.contains(ref.p.path)) {
      return
    }

    if (trace.contains(ref.p.path)) {
      reportError("Found cycle imports in %s", trace.mkString(", "))
    }

    trace += ref.p.path
    for (d <- ref.imports) {
      doCycleCheck(trace, d.ref)
    }
    trace -= ref.p.path
  }
}