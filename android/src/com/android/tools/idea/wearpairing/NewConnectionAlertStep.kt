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
package com.android.tools.idea.wearpairing

import com.android.tools.idea.wizard.model.ModelWizardStep
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.UIUtil.ComponentStyle.LARGE
import icons.StudioIcons
import org.jetbrains.android.util.AndroidBundle.message
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.NORTH
import java.awt.GridBagConstraints.REMAINDER
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class NewConnectionAlertStep(
  model: WearDevicePairingModel,
  val project: Project
) : ModelWizardStep<WearDevicePairingModel>(model, "") {
  private val mainPanel = JPanel()

  override fun shouldShow(): Boolean {
    val selectedPhone = model.selectedPhoneDevice.valueOrNull ?: return false
    val selectedWear = model.selectedWearDevice.valueOrNull ?: return false

    if (model.getNonSelectedRunningWearEmulators().isNotEmpty()) {
      return true
    }

    val (pairedPhone, pairedWear) = WearPairingManager.getPairedDevices()
    return pairedPhone != null && pairedWear != null &&
           (pairedPhone.deviceID != selectedPhone.deviceID || pairedWear.deviceID != selectedWear.deviceID)
  }

  override fun onEntering() {
    // Should always have a value here, otherwise shouldShow() should have returned false
    val selectedPhoneName = model.selectedPhoneDevice.value.displayName
    val selectedWearName = model.selectedWearDevice.value.displayName

    if (model.getNonSelectedRunningWearEmulators().isNotEmpty()) {
      showUi(
        header = message("wear.assistant.connection.alert.close.emulators.title"),
        description = message("wear.assistant.connection.alert.close.emulators.subtitle", selectedWearName, selectedPhoneName)
      )
    }
    else {
      val (pairedPhone, pairedWear) = WearPairingManager.getPairedDevices()
      val pairedPhoneName = pairedPhone?.displayName ?: ""
      val pairedWearName = pairedWear?.displayName ?: ""
      showUi(
        header = message("wear.assistant.connection.alert.disconnect.pairing.title"),
        description = message("wear.assistant.connection.alert.disconnect.pairing.subtitle",
                              selectedWearName, selectedPhoneName, pairedWearName, pairedPhoneName)
      )
    }
  }

  override fun getComponent(): JComponent = mainPanel

  private fun showUi(header: String, description: String) = mainPanel.apply {
    removeAll()

    layout = GridBagLayout()
    border = empty(24)

    add(
      JBLabel(header, LARGE).withFont(JBFont.label().asBold()).withBorder(empty(0, 0, 24, 0)),
      gridConstraint(x = 0, y = 0, fill = HORIZONTAL, gridwidth = REMAINDER)
    )
    add(
      JBLabel(IconUtil.scale(StudioIcons.Common.WARNING, null, 2f)).withBorder(empty(0, 0, 0, 8)),
      gridConstraint(x = 0, y = 1)
    )
    add(JBLabel(description), gridConstraint(x = 1, y = 1, weightx = 1.0, fill = HORIZONTAL)
    )
    add(JPanel(), gridConstraint(x = 0, y = 3, weighty = 1.0)) // Bottom padding

    revalidate()
    repaint()
  }
}

internal fun gridConstraint(
  x: Int, y: Int, weightx: Double = 0.0, weighty: Double = 0.0, fill: Int = NONE, gridwidth: Int = 1, anchor: Int = NORTH
): GridBagConstraints =
  GridBagConstraints().apply {
    this.gridx = x
    this.gridy = y
    this.weightx = weightx
    this.weighty = weighty
    this.fill = fill
    this.gridwidth = gridwidth
    this.anchor = anchor
  }