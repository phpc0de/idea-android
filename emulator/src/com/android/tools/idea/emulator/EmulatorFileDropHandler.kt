/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.emulator

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.tools.deployer.AdbClient
import com.android.tools.deployer.AdbClient.InstallResult
import com.android.tools.deployer.ApkInstaller
import com.android.tools.deployer.InstallStatus
import com.android.tools.idea.adb.AdbService
import com.android.tools.idea.concurrency.addCallback
import com.android.tools.idea.concurrency.transform
import com.android.tools.idea.log.LogWrapper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.ide.dnd.DnDDropHandler
import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDSupport
import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService
import com.intellij.util.concurrency.EdtExecutorService
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.io.File
import java.util.EnumSet
import javax.swing.JComponent

/**
 * Installs a drop handler that installs .apk files and copies all other files to the emulator's Download
 * directory. The lifetime of the drop handler is determined by the lifetime of [emulatorView].
 *
 * @param dropTarget the drop target component
 * @param emulatorView the view associated with the emulator
 * @param project the project associated with [emulatorView]
 */
fun installFileDropHandler(dropTarget: JComponent, emulatorView: EmulatorView, project: Project) {
  DnDSupport.createBuilder(dropTarget)
    .enableAsNativeTarget()
    .setTargetChecker { event ->
      if (FileCopyPasteUtil.isFileListFlavorAvailable(event)) {
        event.isDropPossible = true
        return@setTargetChecker false
      }
      return@setTargetChecker true
    }
    .setDisposableParent(emulatorView)
    .setDropHandler(EmulatorFileDropHandler(emulatorView, project))
    .install()
}

/**
 * Drop handler that installs .apk files and pushes other files to the AVD.
 */
private class EmulatorFileDropHandler(private val emulatorView: EmulatorView, private val project: Project) : DnDDropHandler {

  override fun drop(event: DnDEvent) {
    val files = FileCopyPasteUtil.getFileListFromAttachedObject(event.attachedObject)
    val fileTypes = getFileTypes(files)
    if (fileTypes.size > 1) {
      notifyOfError("Drag-and-drop can either install an APK or copy one or more non-APK files to the Emulator SD card.")
      return
    }

    val fileNames = files.joinToString(", ") { it.name }

    if (fileTypes.contains(FileType.APK)) {
      emulatorView.showLongRunningOperationIndicator("Installing $fileNames")

      val resultFuture: ListenableFuture<InstallResult> = findDevice().transform(getAppExecutorService()) { install(files, it) }

      resultFuture.addCallback(
        EdtExecutorService.getInstance(),
        success = { installResult ->
          if (installResult!!.status == InstallStatus.OK) {
            notifyOfSuccess("$fileNames installed")
          }
          else {
            val message = installResult.reason ?: ApkInstaller.message(installResult)
            notifyOfError(message)
          }
          emulatorView.hideLongRunningOperationIndicator()
        },
        failure = { throwable ->
          val message = throwable?.message ?: "Installation failed"
          notifyOfError(message)
          emulatorView.hideLongRunningOperationIndicator()
        })
    }
    else {
      emulatorView.showLongRunningOperationIndicator("Copying $fileNames")

      val resultFuture: ListenableFuture<Unit> = findDevice().transform(getAppExecutorService()) { push(files, it) }

      resultFuture.addCallback(
        EdtExecutorService.getInstance(),
        success = {
          notifyOfSuccess("$fileNames copied")
          emulatorView.hideLongRunningOperationIndicator()
        },
        failure = { throwable ->
          val message = throwable?.message ?: "Copying failed"
          notifyOfError(message)
          emulatorView.hideLongRunningOperationIndicator()
        })
    }
  }

  private fun getFileTypes(files: List<File>): Set<FileType> {
    val types = EnumSet.noneOf(FileType::class.java)
    for (file in files) {
      types.add(if (file.name.endsWith(".apk", ignoreCase = true)) FileType.APK else FileType.OTHER)
      if (types.contains(FileType.APK) && types.contains(FileType.OTHER)) {
        break
      }
    }
    return types
  }

  private fun install(files: List<File>, device: IDevice): InstallResult {
    val filePaths = files.map(File::getPath).toList()
    return createAdbClient(device).install(filePaths, listOf("-t", "--user", "current", "--full", "--dont-kill"), true)
  }

  private fun push(files: List<File>, device: IDevice) {
    val filePaths = files.map(File::getAbsolutePath).toTypedArray()
    device.push(filePaths, DEVICE_DOWNLOAD_DIR)
  }

  private fun createAdbClient(device: IDevice) = AdbClient(device, LogWrapper(EmulatorFileDropHandler::class.java))

  private fun notifyOfSuccess(message: String) {
    EMULATOR_NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION).notify(project)
  }

  private fun notifyOfError(message: String) {
    EMULATOR_NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).notify(project)
  }

  private fun findDevice(): ListenableFuture<IDevice> {
    val serialNumber = "emulator-${emulatorView.emulator.emulatorId.serialPort}"
    val adbFile = AndroidSdkUtils.getAdb(project) ?:
                  return Futures.immediateFailedFuture(RuntimeException("Could not find adb executable"))
    val bridgeFuture: ListenableFuture<AndroidDebugBridge> = AdbService.getInstance().getDebugBridge(adbFile)
    return bridgeFuture.transform(getAppExecutorService()) { debugBridge ->
      debugBridge.devices.find { it.isEmulator && it.serialNumber == serialNumber } ?:
          throw RuntimeException("Unable to find the device to copy the files to")
    }
  }

  private enum class FileType { APK, OTHER }
}

private const val DEVICE_DOWNLOAD_DIR = "/sdcard/Download"
