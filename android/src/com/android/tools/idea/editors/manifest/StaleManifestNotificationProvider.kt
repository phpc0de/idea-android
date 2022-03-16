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
package com.android.tools.idea.editors.manifest

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications

class StaleManifestNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {
  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (fileEditor !is ManifestEditor || !fileEditor.isShowingStaleManifest) {
      return null
    }
    return EditorNotificationPanel(fileEditor).also {
      it.setText(
        if (fileEditor.failedToComputeFreshManifest()) {
          FAILED_TO_RECOMPUTE_MESSAGE
        }
        else {
          RECOMPUTING_MESSAGE
        }
      )
    }
  }

  companion object {
    private val KEY = Key.create<EditorNotificationPanel>("stale.manifest.notification.panel")

    const val FAILED_TO_RECOMPUTE_MESSAGE = "Failed to compute the merged manifest. The manifest shown here may be out of date."

    const val RECOMPUTING_MESSAGE = "The manifest shown here may be out of date. Recomputing the merged manifest in the background..."
  }
}