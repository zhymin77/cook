package cook.config

import cook.error.ErrorTracking

import java.io.PrintWriter
import scala.io.Source
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path


/**
 * Translate Cook config file to scala source.
 *
 * @author iamtimgreen@gmail.com (Tim Green)
 */
object ConfigScalaSourceGenerator extends ErrorTracking {

  def generate(configRef: ConfigRef): Path = {
    val source = configRef.configScalaSourceFile

    withWriter(source) { writer =>
      generateHeader(configRef, writer)
      if (configRef.configType != ConfigType.CookRootConfig) {
        generateImports(configRef, writer)
      }
      generateBody(configRef, writer)
      generateFooter(configRef, writer)
    }

    source
  }

  private def withWriter(source: Path)(op: PrintWriter => Unit) {
    source.parent.createDirectory()
    source.createFile(true)
    val writer = new PrintWriter(source.jfile)
    op(writer)
    writer.close
  }

  private def generateHeader(configRef: ConfigRef, writer: PrintWriter) {
    writer.println("// GENERATED CODE, DON'T MODIFY")
    writer.println("package %s {  // PACKAGE START" format (configRef.configClassPackageName))

    configRef.configType match {
      case ConfigType.CookConfig =>
        writer.println("trait %s extends %s {  // TRAIT START".format(
          configRef.configClassTraitName,
          ConfigRef.rootConfigRef.configClassTraitFullName
        ))
      case ConfigType.CookiConfig =>
        writer.println("trait %s {  // TRAIT START" format (configRef.configClassTraitName))
      case ConfigType.CookRootConfig =>
        writer.println("// TRAIT START")
        writer.println("trait %s extends xxx".format(configRef.configClassTraitName))
        for (ref <- configRef.mixins) {
          writer.println("  with %s" format ref.configClassTraitFullName)
        }
        writer.println("{")
    }

  }

  private def generateImports(configRef: ConfigRef, writer: PrintWriter) {
    writer.println("// IMPORTS START")

    for (importDefine <- configRef.imports) {
      writer.println("val %s = %s" format (importDefine.name, importDefine.ref.configClassFullName))
      if (importDefine.importMembers) {
        if (ConfigRef.rootConfigRef.mixins.contains(importDefine.ref)) {
          reportError("Already mixin %s in COOK_ROOT, remove @import or use @val instead",
            importDefine.ref.p.path)
        }
        writer.println("import %s._" format (importDefine.name))
      }
    }
    writer.println("// IMPORTS END")
  }

  private def generateBody(configRef: ConfigRef, writer: PrintWriter) {
    writer.println("// BODY START")
    Source.fromFile(configRef.p.path) getLines() foreach writer.println
    writer.println("// BODY END")
  }

  private def generateFooter(configRef: ConfigRef, writer: PrintWriter) {
    writer.println("}  // TRAIT END")

    configRef.configType match {
      case ConfigType.CookRootConfig =>
        // nothing to do
      case ConfigType.CookConfig =>
        writer.println("class %s extends %s".format(
          configRef.configClassName, configRef.configClassTraitName))
      case ConfigType.CookiConfig =>
        writer.println("object %s extends %s".format(
          configRef.configClassName, configRef.configClassTraitName))
    }

    writer.println("}  // PACKAGE END")
  }
}
