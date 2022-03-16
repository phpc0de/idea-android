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
package com.android.tools.idea.emulator.actions

import com.android.emulator.control.PaneEntry.PaneIndex
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Shows the virtual sensors page of the emulator extended controls.
 */
class EmulatorShowFoldingControlsAction : AbstractEmulatorAction() {

  override fun actionPerformed(event: AnActionEvent) {
    val emulatorController = getEmulatorController(event) ?: return
    showExtendedControls(emulatorController, getProject(event), PaneIndex.VIRT_SENSORS)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isVisible = isEmulatorConnected(event) && isFoldableOrRollable(event)
  }

  private fun isFoldableOrRollable(event: AnActionEvent): Boolean {
    val config = getEmulatorController(event)?.emulatorConfig ?: return false
    return config.foldable || config.rollable
  }
}
