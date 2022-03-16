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
package com.android.tools.idea.ui.resourcechooser.colorpicker2

import java.awt.Color
import javax.swing.JButton

class ColorPipetteButton(private val colorPickerModel: ColorPickerModel, private val pipette: ColorPipette) : JButton() {

  init {
    isRolloverEnabled = true

    icon = pipette.icon
    rolloverIcon = pipette.rolloverIcon
    pressedIcon = pipette.pressedIcon

    addActionListener { pipette.pick(MyCallback()) }
  }

  private inner class MyCallback : ColorPipette.Callback {
    override fun picked(pickedColor: Color) = colorPickerModel.setColor(pickedColor, this@ColorPipetteButton)
  }
}
