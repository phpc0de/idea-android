/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.AppUIUtil.invokeLaterIfProjectAlive
import com.intellij.ui.EditorNotifications

private val LOG = Logger.getInstance(StateChangeNotification::class.java)
open class StateChangeNotification(private val project: Project) : GradleSyncListener {

  override fun syncStarted(project: Project) = notifyStateChanged()
  override fun syncSucceeded(project: Project) = notifyStateChanged()
  override fun syncFailed(project: Project, errorMessage: String) = notifyStateChanged()
  override fun syncSkipped(project: Project) = Unit

  @VisibleForTesting
  open fun notifyStateChanged() {
    invokeLaterIfProjectAlive(project) {
      val editorNotifications = EditorNotifications.getInstance(project)
      FileEditorManager.getInstance(project).openFiles.forEach { file ->
        try {
          editorNotifications.updateNotifications(file)
        }
        catch (e: Throwable) {
          LOG.info("Failed to update editor notifications for file '${FileUtil.toSystemDependentName(
            file.path)}'", e)
        }
      }
    }
  }
}