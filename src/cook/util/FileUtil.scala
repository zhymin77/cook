package cook.util

import java.io.File
import java.io.FileNotFoundException

object FileUtil {

  val COOK_ROOT_FILENAME = "COOK_ROOT"

  def findRootDir(currentDir: File): File = {
    if (root != null) return root

    var dir = currentDir.getAbsoluteFile
    do {
      if (isRootDir(dir)) {
        setRoot(dir)
        return dir
      }
      dir = dir.getParentFile
    } while (dir != null)

    throw new FileNotFoundException(
        "Can not find Cook Build Root, from dir %s".format(currentDir.getPath))
  }

  private[util]
  var root: File = null

  def isRootDir(dir: File): Boolean = new File(dir, COOK_ROOT_FILENAME).exists

  def setRoot(root: File) {
    this.root = root.getAbsoluteFile
  }

}