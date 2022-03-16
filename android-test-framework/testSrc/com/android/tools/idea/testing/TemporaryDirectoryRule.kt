// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.android.tools.idea.testing

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.catchAndStoreExceptions
import com.intellij.util.SmartList
import com.intellij.util.io.Ksuid
import com.intellij.util.io.delete
import com.intellij.util.io.exists
import com.intellij.util.io.sanitizeFileName
import com.intellij.util.throwIfNotEmpty
import org.jetbrains.annotations.ApiStatus
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates

/**
 * This is a a slightly modified version of [com.intellij.testFramework.TemporaryDirectory] with
 * a workaround for https://youtrack.jetbrains.com/issue/IDEA-260055.
 *
 * The fileName argument is not used as is for generated file or dir name - sortable UID is added as suffix.
 * `hello.kt` will be created as `hello_1eSBtxBR5522COEjhRLR6AEz.kt`.
 * `.kt` will be created as `1eSBtxBR5522COEjhRLR6AEz.kt`.
 */
class TemporaryDirectoryRule : ExternalResource() {
  private val paths = SmartList<Path>()
  private var sanitizedName: String by Delegates.notNull()

  private var virtualFileRoot: VirtualFile? = null
  private var root: Path? = null

  companion object {
    @JvmStatic
    @JvmOverloads
    fun generateTemporaryPath(fileName: String, root: Path = Paths.get(FileUtilRt.getTempDirectory())): Path {
      val path = root.resolve(generateName(fileName))
      if (path.exists()) {
        throw IllegalStateException("Path $path must be unique but already exists")
      }
      return path
    }

    @JvmStatic
    fun testNameToFileName(name: String): String {
      // remove prefix `test` or `test `
      // ` symbols causes git tests failures, even if it is a valid symbol for file name
      return sanitizeFileName(name.removePrefix("test").trimStart(), extraIllegalChars = { it == ' ' || it == '\'' })
    }

    @JvmStatic
    @ApiStatus.Internal
    fun createVirtualFile(parent: VirtualFile, exactFileName: String, data: String?): VirtualFile {
      return WriteAction.computeAndWait<VirtualFile, IOException> {
        val result = parent.createChildData(TemporaryDirectoryRule::class.java, exactFileName)
        if (data != null && data.isNotEmpty()) {
          result.setBinaryContent(data.toByteArray(Charsets.UTF_8))
        }
        result
      }
    }
  }

  override fun apply(base: Statement, description: Description): Statement {
    sanitizedName = testNameToFileName(description.methodName)
    root = Paths.get(FileUtilRt.getTempDirectory())
    return super.apply(base, description)
  }

  @ApiStatus.Internal
  fun init(commonPrefix: String, root: Path) {
    if (this.root != null) {
      throw IllegalStateException("Already initialized (root=${this.root})")
    }

    sanitizedName = commonPrefix
    this.root = root
  }

  private fun getVirtualRoot(): VirtualFile {
    var result = virtualFileRoot
    if (result == null) {
      val nioRoot = root!!
      Files.createDirectories(nioRoot)
      result = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(nioRoot)
               ?: throw IllegalStateException("Cannot find virtual file by $nioRoot")
      virtualFileRoot = result
    }
    return result
  }

  override fun after() {
    virtualFileRoot = null
    root = null
    if (paths.isEmpty()) {
      return
    }

    val errors = mutableListOf<Throwable>()
    for (i in (paths.size - 1) downTo 0) {
      errors.catchAndStoreExceptions { deleteRecursively(paths[i]) }
    }

    paths.clear()
    throwIfNotEmpty(errors)
  }

  private fun deleteRecursively(path: Path) {
    val maxAttemptCount = 10
    var attemptCount = 0
    while (true) {
      try {
        path.delete()
        return
      }
      catch (e: FileSystemException) {
        if (!SystemInfoRt.isWindows) {
          throw e
        }
        // Deletion of files and directories on Windows is affected by open file handles and may trigger
        // various exceptions, but in most cases would succeed after one or more retries.
        // Examples of exceptions that are being retried here:
        //   AccessDeniedException, DirectoryNotEmptyException, NoSuchFileException,
        //   FileSystemException("The process cannot access the file because it is being used by another process.")
        if (++attemptCount == maxAttemptCount) {
          throw e
        }

        if (e !is NoSuchFileException) {
          try {
            Thread.sleep(10)
          }
          catch (ignored: InterruptedException) {
            throw e
          }
        }
      }
    }
  }

  @JvmOverloads
  fun newPath(fileName: String? = null, refreshVfs: Boolean = false): Path {
    val path = generatePath(fileName)
    if (refreshVfs) {
      path.refreshVfs()
    }
    return path
  }

  /**
   * Use only if you really need a virtual file and not possible to use [Path] (see [newPath]).
   */
  @JvmOverloads
  fun createVirtualFile(fileName: String? = null, data: String? = null): VirtualFile {
    val result = createVirtualFile(getVirtualRoot(), generateName(fileName ?: ""), data)
    paths.add(result.toNioPath())
    return result
  }

  /**
   * Use only and only if you really need virtual file and no way to use [Path] (see [newPath]).
   */
  @JvmOverloads
  fun createVirtualDir(dirName: String? = null): VirtualFile {
    val virtualFileRoot = getVirtualRoot()
    return WriteAction.computeAndWait<VirtualFile, IOException> {
      val name = generateName(dirName ?: "")
      val result = virtualFileRoot.createChildDirectory(TemporaryDirectoryRule::class.java, name)
      paths.add(result.toNioPath())
      result
    }
  }

  @Deprecated(message = "Do not use, only for backward compatibility only.")
  fun scheduleDelete(path: Path) {
    paths.add(path)
  }

  private fun generatePath(suffix: String?): Path {
    var fileName = sanitizedName
    if (suffix != null) {
      fileName = if (fileName.isEmpty()) suffix else "${fileName}_$suffix"
    }

    val path = generateTemporaryPath(fileName, root ?: throw IllegalStateException("not initialized yet"))
    paths.add(path)
    return path
  }
}

fun VirtualFile.writeChild(relativePath: String, data: String) = VfsTestUtil.createFile(this, relativePath, data)

fun VirtualFile.writeChild(relativePath: String, data: ByteArray) = VfsTestUtil.createFile(this, relativePath, data)

fun Path.refreshVfs() {
  // If a temp directory is reused from some previous test run, there might be cached children in its VFS. Ensure they're removed.
  val virtualFile = (LocalFileSystem.getInstance() ?: return).refreshAndFindFileByNioFile(this) ?: return
  VfsUtil.markDirtyAndRefresh(false, true, true, virtualFile)
}

private fun generateName(fileName: String): String {
  // use unique postfix sortable by timestamp to avoid stale data in VFS and file exists check
  // (file is not created at the moment of path generation)
  val nameBuilder = StringBuilder(fileName.length + /* _ separator length */ 1 + Ksuid.MAX_ENCODED_LENGTH)
  val extIndex = fileName.lastIndexOf('.')
  if (fileName.isNotEmpty() && extIndex != 0) {
    if (extIndex == -1) {
      nameBuilder.append(fileName)
    }
    else {
      nameBuilder.append(fileName, 0, extIndex)
    }
    nameBuilder.append('_')
  }
  nameBuilder.append(Ksuid.generate())
  if (extIndex != -1) {
    nameBuilder.append(fileName, extIndex, fileName.length)
  }
  return nameBuilder.toString()
}