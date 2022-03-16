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
package com.android.tools.idea.gradle.structure.configurables.ui.modules

import com.android.tools.idea.gradle.structure.configurables.ConfigurablesTreeModel
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.configurables.ui.AbstractTabbedMainPanel
import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule

const val MODULE_PLACE_NAME = "android.psd.module"
class ModulePanel(
    context: PsContext,
    module: PsAndroidModule,
    signingConfigsTreeModel: ConfigurablesTreeModel
) : AbstractTabbedMainPanel(context, placeName = MODULE_PLACE_NAME) {

  private val modulePropertiesConfigPanel = ModulePropertiesConfigPanel(module, context)
  private val moduleDefaultConfigConfigPanel = ModuleDefaultConfigConfigPanel(module.defaultConfig, context)
  private val moduleSigningConfigsPanel = SigningConfigsPanel(module, signingConfigsTreeModel, context.uiSettings)

  init {
    addTab(modulePropertiesConfigPanel)
    addTab(moduleDefaultConfigConfigPanel)
    addTab(moduleSigningConfigsPanel)
  }

  override fun PsUISettings.getLastSelectedTab(): String? = MODULE_TAB

  override fun PsUISettings.setLastSelectedTab(value: String) {
    MODULE_TAB = value
  }
}
