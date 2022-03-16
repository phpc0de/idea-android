/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.npw.java

import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.device.FormFactor
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.npw.labelFor
import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.validator.ClassNameValidator
import com.android.tools.idea.npw.verticalGap
import com.android.tools.idea.observable.ui.TextProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI.Borders.empty
import org.jetbrains.android.util.AndroidBundle
import javax.swing.JTextField

class ConfigureLibraryModuleStep(
  model: NewLibraryModuleModel, title: String
) : ConfigureModuleStep<NewLibraryModuleModel>(
  model, FormFactor.MOBILE, SdkVersionInfo.LOWEST_ACTIVE_API, title = title
) {
  private val className: JTextField = JBTextField()

  override fun createMainPanel(): DialogPanel = panel {
    row {
      labelFor("Library name:", moduleName, AndroidBundle.message("android.wizard.module.help.name"))
      moduleName(pushX)
    }
    row {
      labelFor("Package name:", packageName)
      packageName(pushX)
    }
    row {
      labelFor("Class name:", className)
      className(pushX)
    }
    row {
      labelFor("Language:", languageCombo)
      languageCombo(growX)
    }
    if (StudioFlags.NPW_SHOW_GRADLE_KTS_OPTION.get()) {
      verticalGap()

      row {
        gradleKtsCheck()
      }
    }
  }.withBorder(empty(6))

  init {
    bindings.bindTwoWay(TextProperty(className), model.className)

    validatorPanel.apply {
      registerValidator(TextProperty(className), ClassNameValidator())
    }
  }

  override fun getPreferredFocusComponent() = moduleName
}
