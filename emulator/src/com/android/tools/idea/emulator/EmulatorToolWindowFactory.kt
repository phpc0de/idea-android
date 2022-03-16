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
package com.android.tools.idea.emulator

import com.android.tools.idea.avdmanager.HardwareAccelerationCheck.isChromeOSAndIsNotHWAccelerated
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.android.util.AndroidUtils.hasAndroidFacets

/**
 * [ToolWindowFactory] implementation for the Emulator tool window.
 */
class EmulatorToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED)
    EmulatorToolWindowManager.initializeForProject(project)
  }

  override fun init(toolWindow: ToolWindow) {
    toolWindow.stripeTitle = EMULATOR_TOOL_WINDOW_TITLE
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    val available = canLaunchEmulator()
    if (available) {
      EmulatorToolWindowManager.initializeForProject(project)
    }
    return available
  }

  private fun canLaunchEmulator(): Boolean {
    return !isChromeOSAndIsNotHWAccelerated()
  }
}