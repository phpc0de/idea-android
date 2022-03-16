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
package com.android.tools.idea.structure

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ex.SingleConfigurableEditor
import com.intellij.openapi.options.newEditor.SettingsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.ui.DialogWrapper

/**
 * Action to open IDEA's project structure dialog, this can be useful when debugging the structure of a project as seen by IDEA.
 */
class IdeaProjectStructureAction : AnAction("IDEA Project Structure Dialog") {

  override fun actionPerformed(e: AnActionEvent) =
    showDialog(e.project ?: ProjectManager.getInstance().defaultProject)

  internal fun showDialog(project: Project) {
    object : SingleConfigurableEditor(project, ProjectStructureConfigurable.getInstance(project), SettingsDialog.DIMENSION_KEY) {
      override fun getStyle() = DialogWrapper.DialogStyle.COMPACT
    }.show()
  }
}