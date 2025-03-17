//> using scala "2.13"
//> using jvm "21"
//> using dep "com.lihaoyi::os-lib::0.11.4"
//> using dep "com.lihaoyi::pprint::0.9.0"

import scala.util.Properties

final class ProcessBuildNinja(cross: Boolean) {

  def isMac = Properties.isMac
  def isWin = cross || Properties.isWin
  def isCrossWin = cross && !Properties.isWin

  val dynExt =
    if (isMac) "dylib"
    else if (isWin) "dll.a"
    else sys.error(s"Unexpected OS (${sys.props("os.name")})")
  private val staticExt =
    if (isMac) "a"
    else if (isWin) "a"
    else sys.error(s"Unexpected OS (${sys.props("os.name")})")

  private def asPath(strPath: String): Option[os.Path] = {
    val strPath0 =
      if (isWin && !isCrossWin) {
        var strPath1 = strPath
        if (strPath1.length() >= 2 && strPath1(1) == '$')
          strPath1 = strPath1.take(1) + strPath1.drop(2)
        strPath1.replace('/', '\\')
      }
      else
        strPath

    os.FilePath(strPath0) match {
      case p: os.Path => Some(p)
      case other =>
        println(s"Ignoring non-absolute path '$strPath'")
        None
    }
  }

  private def strPath(path: os.Path, addDollar: Boolean = false): String =
    if (isWin && !isCrossWin) {
      val str = path.toString.replace("\\", "/")
      if (addDollar) str.take(1) + "$" + str.drop(1)
      else str
    }
    else
      path.toString

  private lazy val isArm64 =
    Option(System.getProperty("os.arch")).map(_.toLowerCase(java.util.Locale.ROOT)) match {
      case Some("aarch64" | "arm64") => true
      case _                         => false
    }
  private lazy val homebrewBase = os.Path(
    if (isArm64) "/opt/homebrew"
    else "/usr/local"
  )

  private lazy val homebrewPcre = {
    val pcre0 = homebrewBase / "opt/pcre2/lib/libpcre2-8.a"
    if (!os.exists(pcre0)) {
      val dir = pcre0 / os.up
      if (os.isDir(dir))
        pprint.err.log(os.list(dir).map(_.relativeTo(dir).asSubPath))
      sys.error(s"$pcre0 not found")
    }
    pcre0
  }

  def updateLib(
    path: String,
    addSystemExtras: Boolean,
    addStaticExtras: Boolean,
    quoted: Boolean = false
  ): String =
    if (path.contains("@")) {
      println(s"Warning: ignoring $path")
      if (quoted) "\"" + path + "\""
      else path
    }
    else
      asPath(path) match {
        case Some(path0) =>
          if (!os.exists(path0))
            sys.error(s"$path not found")
          val ar = path.stripSuffix("." + dynExt) + "." + staticExt
          val ar0 = asPath(ar).getOrElse(sys.error(s"should not happen ($path, $ar)"))
          if (os.exists(ar0)) {
            val extra =
              if (addSystemExtras && isMac && path0.last.startsWith("libglib-2.0"))
                Seq(
                  "-liconv",
                  "-framework", "CoreFoundation",
                  "-lobjc",
                  "-framework", "Foundation",
                  "-framework", "AppKit",
                  "-framework", "CoreServices",
                  "-lffi",
                  homebrewPcre.toString,
                  "-lresolv"
                )
              else if (addStaticExtras && isMac && path0.last.startsWith("libglib-2.0"))
                Seq(
                  homebrewPcre.toString
                )
              else if (addSystemExtras && isMac && path0.last.startsWith("libgio-2.0"))
                Seq(
                  ((ar0 / os.up) / "libgmodule-2.0.a").toString,
                  "-lz"
                )
              else if ((addSystemExtras || addStaticExtras) && isWin && path0.last.startsWith("libglib-2.0")) {
                val pcre = path0 / os.up / "libpcre2-8.a"
                Seq(
                  pcre
                ).map(strPath(_, addDollar = path(1) == '$'))
              }
              else if (addSystemExtras && isWin && path0.last.startsWith("libintl")) {
                val iconv = (ar0 / os.up) / "libiconv.a"
                Seq(
                  iconv
                ).map(strPath(_, addDollar = path(1) == '$'))
              }
              else if (addSystemExtras && isWin && path0.last.startsWith("libgio-2.0")) {
                val gmod = (ar0 / os.up) / "libgmodule-2.0.a"
                val iphlpapi = (ar0 / os.up) / "libiphlpapi.a"
                val dnsapi = (ar0 / os.up) / "libdnsapi.a"
                val shlwapi = (ar0 / os.up) / "libshlwapi.a"
                val zlib = (ar0 / os.up) / "libz.a"
                Seq(
                  gmod,
                  iphlpapi,
                  dnsapi,
                  shlwapi,
                  zlib
                ).map(strPath(_, addDollar = path(1) == '$'))
              }
              else if (addSystemExtras && isWin && path0.last.startsWith("libgobject-2.0")) {
                val ffi = (ar0 / os.up) / "libffi.a"
                Seq(
                  ffi
                ).map(strPath(_, addDollar = path(1) == '$'))
              }
              else
                Nil
            val substitute = (ar +: extra)
              .map {
                if (quoted)
                  s => "\"" + s + "\""
                else
                  s => s
              }
              .mkString(" ")
            println(s"Replacing $path by $substitute")
            substitute
          }
          else {
            println(s"Warning: $ar not found")
            val dir = path0 / os.up
            val dirContent = os.walk(dir).map(_.relativeTo(dir).asSubPath)
            pprint.err.log(dirContent)
            if (quoted) "\"" + path + "\""
            else path
          }
        case None =>
          if (quoted) "\"" + path + "\""
          else path
      }
}

object ProcessBuildNinja {
  def main(args: Array[String]): Unit = {
    val (cross, targets) =
      if (args.headOption.contains("--cross")) (true, args.drop(1).toSeq)
      else (false, args.toSeq)
      args.toSeq
    if (targets.isEmpty)
      sys.error("No targets passed as argument")

    val buildNinja = os.pwd / "build/build.ninja"
    if (!os.exists(buildNinja))
      sys.error(s"$buildNinja not found")

    val helper = new ProcessBuildNinja(cross)

    var processedTarget = Set.empty[String]

    val content = os.read(buildNinja)
    val it = content.linesIterator.zip(content.linesWithSeparators).zipWithIndex.map {
      case ((line, lineWithSep), idx) =>
        try {
          val targetOpt = targets.find { target =>
            line.startsWith(s"build $target: ")
          }
          if (targetOpt.nonEmpty || line.startsWith(" LINK_ARGS = ")) {
            val elems = line.split(" ", -1).toSeq
            if (elems.mkString(" ") != line) {
              pprint.err.log(line)
              pprint.err.log(elems.mkString(" "))
              sys.error("Consistency error")
            }
            val elems0 = elems.map {
              case dynLib if dynLib.endsWith("." + helper.dynExt) =>
                helper.updateLib(
                  dynLib,
                  addSystemExtras = line.startsWith(" LINK_ARGS = "),
                  addStaticExtras = line.startsWith("build ")
                )
              case quotedDynLib if quotedDynLib.startsWith("\"") && quotedDynLib.endsWith("." + helper.dynExt + "\"") =>
                val dynLib = quotedDynLib.stripPrefix("\"").stripSuffix("\"")
                helper.updateLib(
                  dynLib,
                  addSystemExtras = line.startsWith(" LINK_ARGS = "),
                  addStaticExtras = line.startsWith("build "),
                  quoted = true
                )
              case other =>
                other
            }
            if (elems0 == elems) {
              if (line.startsWith("build ")) {
                System.err.println(s"Nothing to substitute in line ${idx + 1}: '$line'")
                pprint.err.log(elems, height = Int.MaxValue)
              }
              lineWithSep
            }
            else {
              for (target <- targetOpt)
                processedTarget += target
              elems0.mkString(" ") + lineWithSep.drop(line.length)
            }
          }
          else {
            // if (line.startsWith("build "))
            //   System.err.println(s"Ignoring build line ${idx + 1}: '${line.take(200)}'")
            lineWithSep
          }
        }
        catch {
          case e: Throwable =>
            throw new Exception(s"Error while processing line ${idx + 1}: '${line.take(200)}'", e)
        }
    }

    val updatedContent = it.mkString

    val missingTargets = targets.filter(target => !processedTarget.contains(target))
    if (missingTargets.nonEmpty)
      sys.error(
        s"${if (missingTargets.lengthCompare(1) > 0) "Targets" else "Target"} not found or don't need processing: " +
          missingTargets.mkString(", ")
      )

    pprint.err.log(targets)
    pprint.err.log(missingTargets)

    os.write.over(buildNinja, updatedContent)
    System.err.println(s"Wrote $buildNinja")
  }
}
