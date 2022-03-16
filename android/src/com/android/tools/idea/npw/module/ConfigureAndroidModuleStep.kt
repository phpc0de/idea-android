/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.npw.module

import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.npw.labelFor
import com.android.tools.idea.npw.model.NewAndroidModuleModel
import com.android.tools.idea.npw.model.RenderTemplateModel
import com.android.tools.idea.npw.model.RenderTemplateModel.Companion.fromModuleModel
import com.android.tools.idea.npw.template.ChooseActivityTypeStep
import com.android.tools.idea.npw.template.components.BytecodeLevelComboProvider
import com.android.tools.idea.npw.toWizardFormFactor
import com.android.tools.idea.npw.validator.ProjectNameValidator
import com.android.tools.idea.npw.verticalGap
import com.android.tools.idea.observable.ui.SelectedItemProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.wizard.model.ModelWizardStep
import com.android.tools.idea.wizard.template.BytecodeLevel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI.Borders.empty
import org.jetbrains.android.util.AndroidBundle.message
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField

class ConfigureAndroidModuleStep(
  model: NewAndroidModuleModel,
  minSdkLevel: Int,
  basePackage: String?,
  title: String
) : ConfigureModuleStep<NewAndroidModuleModel>(model, model.formFactor.get().toWizardFormFactor(), minSdkLevel, basePackage, title) {
  private val appName: JTextField = JBTextField(model.applicationName.get())
  private val bytecodeCombo: JComboBox<BytecodeLevel> = BytecodeLevelComboProvider().createComponent()

  override fun createMainPanel(): DialogPanel = panel {
    if (!model.isLibrary) {
      row {
        labelFor("Application/Library name", appName)
        appName(pushX)
      }
    }

    row {
      labelFor("Module name", moduleName, message("android.wizard.module.help.name"))
      moduleName(pushX)
    }

    row {
      labelFor("Package name", packageName)
      packageName(pushX)
    }

    row {
      labelFor("Language", languageCombo)
      languageCombo(growX)
    }

    if (model.isLibrary) {
      row {
        labelFor("Bytecode Level", bytecodeCombo)
        bytecodeCombo(growX)
      }
    }

    row {
      labelFor("Minimum SDK", apiLevelCombo)
      apiLevelCombo(growX)
    }

    if (StudioFlags.NPW_SHOW_GRADLE_KTS_OPTION.get()) {
      verticalGap()

      row {
        gradleKtsCheck()
      }
    }
  }.withBorder(empty(6))

  init {
    bindings.bindTwoWay(TextProperty(appName), model.applicationName)
    bindings.bindTwoWay(SelectedItemProperty(bytecodeCombo), model.bytecodeLevel)
    validatorPanel.registerValidator(model.applicationName, ProjectNameValidator())
  }

  override fun createDependentSteps(): Collection<ModelWizardStep<*>> {
    val commonSteps = super.createDependentSteps()

    val renderModel: RenderTemplateModel = fromModuleModel(model, message("android.wizard.activity.add", formFactor.id))
    // Note: MultiTemplateRenderer needs that all Models constructed (ie myRenderModel) are inside a Step, so handleSkipped() is called
    val chooseActivityStep = ChooseActivityTypeStep.forNewModule(renderModel, formFactor, model.androidSdkInfo)
    chooseActivityStep.setShouldShow(!model.isLibrary)
    return listOf(chooseActivityStep) + commonSteps
  }

  override fun getPreferredFocusComponent(): JComponent = if (appName.isVisible) appName else moduleName
}