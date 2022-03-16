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
package com.android.tools.idea.emulator.settings

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.emulator.DEFAULT_SNAPSHOT_AUTO_DELETION_POLICY
import com.android.tools.idea.emulator.EmulatorSettings
import com.android.tools.idea.emulator.EmulatorSettings.SnapshotAutoDeletionPolicy
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import org.jetbrains.annotations.Nls
import javax.swing.JCheckBox

/**
 * Implementation of Settings > Tools > Emulator preference page.
 */
class EmulatorSettingsUi : SearchableConfigurable, Configurable.NoScroll {

  private lateinit var launchInToolWindowCheckBox: JCheckBox
  private lateinit var snapshotAutoDeletionPolicyComboBox: ComboBox<SnapshotAutoDeletionPolicy>
  private val snapshotAutoDeletionPolicyComboBoxModel = EnumComboBoxModel(SnapshotAutoDeletionPolicy::class.java)

  private val state = EmulatorSettings.getInstance()

  override fun getId() = "emulator.options"

  override fun createComponent() = panel {
    row {
      launchInToolWindowCheckBox =
          checkBox("Launch in a tool window",
                   comment = "Enabling this setting will cause Android Emulator to launch in a tool window. " +
                             "Otherwise Android Emulator will launch as a standalone application.").component
    }
    blockRow {} // Visual separator.
    row {
      cell(isVerticalFlow = true) {
        label("When encountering snapshots incompatible with the current configuration:")
        snapshotAutoDeletionPolicyComboBox =
            comboBox(snapshotAutoDeletionPolicyComboBoxModel,
                     { snapshotAutoDeletionPolicyComboBoxModel.selectedItem },
                     { snapshotAutoDeletionPolicyComboBoxModel.setSelectedItem(it) },
                     renderer = SimpleListCellRenderer.create(DEFAULT_SNAPSHOT_AUTO_DELETION_POLICY.displayName) { it?.displayName })
              .enableIf(launchInToolWindowCheckBox.selected)
              .component
      }
    }
  }

  override fun isModified(): Boolean {
    return launchInToolWindowCheckBox.isSelected != state.launchInToolWindow ||
           snapshotAutoDeletionPolicyComboBoxModel.selectedItem != state.snapshotAutoDeletionPolicy
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    state.launchInToolWindow = launchInToolWindowCheckBox.isSelected
    state.snapshotAutoDeletionPolicy = snapshotAutoDeletionPolicyComboBoxModel.selectedItem
  }

  override fun reset() {
    launchInToolWindowCheckBox.isSelected = state.launchInToolWindow
    snapshotAutoDeletionPolicyComboBoxModel.setSelectedItem(state.snapshotAutoDeletionPolicy)
  }

  @Nls
  override fun getDisplayName() = if (IdeInfo.getInstance().isAndroidStudio) "Emulator" else "Android Emulator"
}