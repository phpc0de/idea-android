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
package com.android.tools.idea.run.deployment

import com.android.tools.idea.run.LaunchCompatibility.State
import com.intellij.CommonBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.panel
import com.intellij.util.IconUtil
import icons.StudioIcons
import org.jetbrains.android.util.AndroidBundle.message
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

/**
 * Displays the deployment issues for each device and a warning or error icon when the user deploys their app.
 *
 * In case of a warning the user can proceed with the deployment and choose not to show this dialog during the current session.
 */
class SelectedDevicesErrorDialog(private val project: Project, private val devices: List<Device>) : DialogWrapper(project) {
  companion object {
    @JvmField
    internal val DO_NOT_SHOW_WARNING_ON_DEPLOYMENT = com.intellij.openapi.util.Key.create<Boolean>("do.not.show.warning.on.deployment")
  }

  private val anyDeviceHasError = devices.any { it.launchCompatibility.state == State.ERROR }

  init {
    setResizable(false)
    if (!anyDeviceHasError) {
      setDoNotAskOption(object : DoNotAskOption.Adapter() {
        override fun rememberChoice(isSelected: Boolean, exitCode: Int) = project.putUserData(DO_NOT_SHOW_WARNING_ON_DEPLOYMENT, isSelected)
        override fun getDoNotShowMessage() = message("do.not.ask.for.this.session")
        override fun isSelectedByDefault() = project.getUserData(DO_NOT_SHOW_WARNING_ON_DEPLOYMENT) == true
      })
    }
    init()
  }

  override fun createActions(): Array<Action> {
    return if (!anyDeviceHasError) {
      myOKAction.putValue(Action.NAME, CommonBundle.getContinueButtonText())
      arrayOf(cancelAction, okAction)
    }
    else {
      myCancelAction.putValue(Action.NAME, CommonBundle.getOkButtonText())
      myCancelAction.putValue(DEFAULT_ACTION, true)
      arrayOf(cancelAction)
    }
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        cell(isVerticalFlow = true) {
          val icon = if (anyDeviceHasError) StudioIcons.Common.ERROR else StudioIcons.Common.WARNING
          component(JBLabel(IconUtil.scale(icon, null, 2.5f)))
        }
        cell(isVerticalFlow = true) {
          val title = if (anyDeviceHasError) message("error.level.title") else message("warning.level.title")
          label(title, bold = true)
          devices.map {
            component(LimitedWidthLabel("${it.launchCompatibility.reason} on device $it"))
          }
        }
      }
    }.withBorder(BorderFactory.createEmptyBorder(16, 0, 0, 16))
  }
}

private class LimitedWidthLabel(val str: String) : JLabel() {
  companion object {
    private const val MAX_WIDTH = 446
  }

  init {
    val v = BasicHTML.createHTMLView(this, HtmlChunk.raw(str).wrapWith(HtmlChunk.html()).toString())
    val width = v.getPreferredSpan(View.X_AXIS)
    val div = if (width > MAX_WIDTH) HtmlChunk.div().attr("width", MAX_WIDTH) else HtmlChunk.div()
    text = div.addRaw(str).wrapWith(HtmlChunk.html()).toString()
  }
}
