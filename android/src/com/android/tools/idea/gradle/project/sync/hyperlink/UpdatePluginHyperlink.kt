/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.hyperlink

import com.android.tools.idea.gradle.project.sync.issues.processor.GradlePluginInfo
import com.android.tools.idea.gradle.project.sync.issues.processor.UpdateGradlePluginProcessor
import com.android.tools.idea.project.hyperlink.NotificationHyperlink
import com.intellij.openapi.project.Project

class UpdatePluginHyperlink(
  val pluginToVersionMap: Map<GradlePluginInfo, String>
) : NotificationHyperlink("update.plugins", "Update plugins") {
  override fun execute(project: Project) {
    val processor = UpdateGradlePluginProcessor(project, pluginToVersionMap)
    processor.setPreviewUsages(true)
    processor.run()
  }
}