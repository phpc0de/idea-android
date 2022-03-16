/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync.internal

import com.android.SdkConstants
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.android.sdklib.devices.Abi
import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.util.EmbeddedDistributionPaths
import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.util.StudioPathManager
import com.android.utils.FileUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.sanitizeFileName
import org.jetbrains.android.facet.AndroidFacetProperties
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.lang.Math.max
import java.util.*

/**
 * A helper class to dump an IDEA project to a stable human readable text format that can be compared in tests.
 */
class ProjectDumper(
  private val offlineRepos: List<File> = getOfflineM2Repositories(),
  private val androidSdk: File = IdeSdks.getInstance().androidSdkPath!!,
  private val devBuildHome: File = getStudioSourcesLocation(),
  private val additionalRoots: Map<String, File> = emptyMap()
) {
  private val gradleCache: File = getGradleCacheLocation()
  private val userM2: File = getUserM2Location()
  private val systemHome = getSystemHomeLocation()

  init {
    println("<DEV>         <== ${devBuildHome.absolutePath}")
    println("<GRADLE>      <== ${gradleCache.absolutePath}")
    println("<ANDROID_SDK> <== ${androidSdk.absolutePath}")
    println("<M2>          <==")
    offlineRepos.forEach {
      println("                  ${it.absolutePath}")
    }
    println("<HOME>        <== ${systemHome.absolutePath}")
    additionalRoots.forEach { (key, value) ->
      println("<$key>        <== ${value.absolutePath}")
    }
  }

  private val output = StringBuilder()

  private var currentRootDirectory: File = File("/")
  private var currentRootDirectoryName = "/"
  private var currentNestingPrefix: String = ""

  private val gradleDistStub = "x".repeat(25)
  private val gradleHashStub = "x".repeat(32)
  private val gradleLongHashStub = "x".repeat(40)
  private val gradleDistPattern = Regex("/[0-9a-z]{${gradleDistStub.length - 3},${gradleDistStub.length}}/")
  private val gradleHashPattern = Regex("[0-9a-f]{${gradleHashStub.length - 3},${gradleHashStub.length}}")
  private val gradleLongHashPattern = Regex("[0-9a-f]{${gradleLongHashStub.length - 3},${gradleLongHashStub.length}}")
  private val gradleVersionPattern = Regex("gradle-.*${SdkConstants.GRADLE_LATEST_VERSION}")
  private val kotlinVersionPattern =
    // org.jetbrains.kotlin:kotlin-smth-smth-smth:1.3.1-eap-23"
    // kotlin-something-1.3.1-eap-23
    Regex("(?:(?:org.jetbrains.kotlin:kotlin(?:-[0-9a-z]*)*:)|(?:kotlin(?:-[0-9a-z]+)*)-)(\\d+\\.\\d+.[0-9a-z\\-]+)")
  private val dotAndroidFolderPathPattern = Regex("^/([_/0-9a-z])+\\.android")

  fun File.normalizeCxxPath(variantName: String?): String {
    val cxxSegment = findCxxSegment(this) ?: return this.path
    val abiSegment = findAbiSegment(this) ?: return this.path
    val stringFile = this.toString().replace("\\", "/")
    val variantSegmentToReplace = stringFile.substring(stringFile.lastIndexOf(cxxSegment) + cxxSegment.length + 1, stringFile.lastIndexOf(abiSegment) - 1)
    val pathsToReplace = mapOf(
      "/build/intermediates/cxx/${variantSegmentToReplace}" to "/<CXX>/{${variantName?.toUpperCase(Locale.ROOT)}}",
      "/build/.cxx/${variantSegmentToReplace}" to "/<CXX>/{${variantName?.toUpperCase(Locale.ROOT)}}",
      "/cxx/${variantSegmentToReplace}" to "/<CXX>/{${variantName?.toUpperCase(Locale.ROOT)}}",
      "/.cxx/${variantSegmentToReplace}" to "/<CXX>/{${variantName?.toUpperCase(Locale.ROOT)}}")
    var result = this.path
    for ((old, new) in pathsToReplace) {
      result = result.replace(old, new)
    }
    return result
  }

  private fun findCxxSegment(file: File): String? {
    val name = file.name
    if (name.endsWith("cxx")) return file.name
    return findCxxSegment(file.parentFile?:return null)
  }

  private fun findAbiSegment(file: File): String? {
    val name = file.name
    if (name in Abi.values().map{it -> it.toString()}.toSet()) return file.name
    return findAbiSegment(file.parentFile?:return null)
  }

  fun String.toPrintablePaths(): Collection<String> =
    split(AndroidFacetProperties.PATH_LIST_SEPARATOR_IN_FACET_CONFIGURATION).map { it.toPrintablePath() }

  /**
   * Replaces well-known instable parts of a path/url string with stubs and adds [-] to the end if the file does not exist.
   */
  fun String.toPrintablePath(): String {
    fun String.splitPathAndSuffix(): Pair<String, String> =
      when {
        this.endsWith("!") -> this.substring(0, this.length - 1) to "!"
        this.endsWith("!/") -> this.substring(0, this.length - 2) to "!/"
        else -> this to ""
      }

    return when {
      this.startsWith("file://") -> "file://" + this.substring("file://".length).toPrintablePath()
      this.startsWith("jar://") -> "jar://" + this.substring("jar://".length).toPrintablePath()
      else -> {
        val (filePath, suffix) = splitPathAndSuffix()
        val file = File(filePath)
        val existenceSuffix = if (!file.exists()) " [-]" else ""
        val maskedPath = (if (file.isRooted) filePath.replaceKnownPaths() else filePath) + suffix + existenceSuffix
        if (IdeInfo.getInstance().isAndroidStudio) maskedPath else convertToMaskedMavenPath(maskedPath)
      }
    }
  }

  @VisibleForTesting
  fun convertToMaskedMavenPath(maskedPath: String): String {
    var res = maskedPath
    val gradleFilesPrefix = "<GRADLE>/caches/modules-2/files-2.1/"
    if (res.startsWith(gradleFilesPrefix)) {
      val pkgEndIndex = res.indexOf('/', gradleFilesPrefix.length)
      val pkg = res.substring(gradleFilesPrefix.length, pkgEndIndex)
      val remaining = res.substring(pkgEndIndex).replace("/$gradleLongHashStub/", "/")
      res = "<M2>/" + pkg.replace('.', '/') + remaining
    }
    return res
  }

  fun String.replaceKnownPaths(): String =
    this
      .let { offlineRepos.fold(it) { text, repo -> text.replace(FileUtils.toSystemIndependentPath(repo.absolutePath), "<M2>", ignoreCase = false) } }
      .let { additionalRoots.entries.fold(it) { text, (name, dir) -> text.replace(dir.absolutePath, "<$name>", ignoreCase = false) } }
      .replace(FileUtils.toSystemIndependentPath(currentRootDirectory.absolutePath), "<$currentRootDirectoryName>", ignoreCase = false)
      .replace(FileUtils.toSystemIndependentPath(gradleCache.absolutePath), "<GRADLE>", ignoreCase = false)
      .replace(FileUtils.toSystemIndependentPath(androidSdk.absolutePath), "<ANDROID_SDK>", ignoreCase = false)
      .replace(FileUtils.toSystemIndependentPath(devBuildHome.absolutePath), "<DEV>", ignoreCase = false)
      .replace(FileUtils.toSystemIndependentPath(userM2.absolutePath), "<USER_M2>", ignoreCase = false)
      .let {
        if (it.contains(gradleVersionPattern)) {
          it.replace(SdkConstants.GRADLE_LATEST_VERSION, "<GRADLE_VERSION>")
        }
        else it
      }
      .replace(gradleLongHashPattern, gradleLongHashStub)
      .replace(gradleHashPattern, gradleHashStub)
      .replace(gradleDistPattern, "/$gradleDistStub/")
      .replace(ANDROID_GRADLE_PLUGIN_VERSION, "<AGP_VERSION>")
      .replace(dotAndroidFolderPathPattern, "<.ANDROID>")
      .let {
        kotlinVersionPattern.find(it)?.let { match ->
          it.replace(match.groupValues[1], "<KOTLIN_VERSION>")
        } ?: it
      }
      .let {
        if (IdeInfo.getInstance().isAndroidStudio) it
        else it.replace("/jetified-", "/", ignoreCase = false) // flaky GradleSyncProjectComparisonTest tests in IDEA
      }
      .removeAndroidVersionsFromPath()

  fun appendln(data: String) {
    output.append(currentNestingPrefix)
    output.appendln(data.trimEnd())
  }

  /**
   * Temporarily configures additional identation and optionally configures a new current directory root which will be replaced
   * with [rootName] in the output and runs [code].
   */
  fun nest(root: File? = null, rootName: String? = null, code: ProjectDumper.() -> Unit) {
    val savedRoot = this.currentRootDirectory
    val savedRootName = this.currentRootDirectoryName
    this.currentRootDirectory = root ?: this.currentRootDirectory
    this.currentRootDirectoryName = rootName ?: this.currentRootDirectoryName
    val saved = currentNestingPrefix
    currentNestingPrefix += "    "
    code()
    currentNestingPrefix = saved
    this.currentRootDirectory = savedRoot
    this.currentRootDirectoryName = savedRootName
  }

  fun String.removeAndroidVersionsFromPath(): String =
    androidPathPattern.find(this)?.groups?.get(1)?.let {
      this.replace(it.value, "<VERSION>")
    } ?: this

  fun String.replaceJavaVersion(): String? = replace(Regex("11|1\\.8"), "<JAVA_VERSION>")
  fun String.replaceJdkVersion(): String? = replace(Regex("1\\.8\\.0_[0-9]+|11\\.0\\.[0-9]+"), "<JDK_VERSION>")
  fun String.replaceMatchingVersion(version: String?): String =
    if (version != null) this.replace("-$version", "-<VERSION>") else this


  fun String.smartPad() = this.padEnd(max(30, 10 + this.length / 10 * 10))
  fun String.markMatching(matching: String) = if (this == matching) "$this [=]" else this

  fun String.removeAndroidVersionsFromDependencyNames(): String =
    androidLibraryPattern.find(this)?.groups?.get(1)?.let {
      this.replaceRange(it.range, "<VERSION>")
    } ?: this

  fun String.getAndroidVersionFromDependencyName(): String? =
    androidLibraryPattern.find(this)?.groups?.get(1)?.value

  override fun toString(): String = output.toString().trimIndent()
}

fun ProjectDumper.prop(name: String, value: () -> String?) {
  value()?.let {
    appendln("${name.smartPad()}: $it")
  }
}

fun ProjectDumper.head(name: String, value: () -> String? = { null }) {
  val v = value()
  appendln(name.smartPad() + if (v != null) ": $v" else "")
}

private fun getGradleCacheLocation() = File(System.getProperty("gradle.user.home") ?:
                                            System.getenv("GRADLE_USER_HOME") ?:
                                            (System.getProperty("user.home") + "/.gradle"))

private fun getStudioSourcesLocation() = File(StudioPathManager.getSourcesRoot())

private fun getSystemHomeLocation() = getStudioSourcesLocation().toPath().parent.toFile()

private fun getUserM2Location() = File(System.getProperty("user.home") + "/.m2/repository")

private fun getOfflineM2Repositories(): List<File> =
    (EmbeddedDistributionPaths.getInstance().findAndroidStudioLocalMavenRepoPaths())
        .map { File(FileUtil.toCanonicalPath(it.absolutePath)) }

/**
 * Replaces artifact version in string containing artifact idslike com.android.group:artifact:28.7.8@aar with <VERSION>.
 */
private val androidLibraryPattern =
  Regex("(?:(?:com\\.android\\.)|(?:android\\.arch\\.))(?:(?:\\w|-)+(?:\\.(?:(?:\\w|-)+))*:(?:\\w|-)+:)([^@ ]*)")

/**
 * Replaces artifact version in string containing artifact ids like com.android.group.artifact.artifact-28.3.4.jar with <VERSION>.
 */
private val androidPathPattern = Regex("(?:com/android/.*/)([0-9.]+)(?:/.*-)(\\1)(?:\\.jar)")

class DumpProjectAction : DumbAwareAction("Dump Project Structure") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val dumper = ProjectDumper()
    dumper.dumpProject(project)
    val dump = dumper.toString().trimIndent()
    val outputFile = File(File(project.basePath), sanitizeFileName(project.name) + ".project_dump")
    outputFile.writeText(dump)
    println("Dumped to: file://$outputFile")
  }
}
