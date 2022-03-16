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

import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.configurables.android.modules.androidModulePropertiesModel
import com.android.tools.idea.gradle.structure.configurables.ui.ModelPanel
import com.android.tools.idea.gradle.structure.configurables.ui.properties.ConfigPanel
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule
import com.google.wireless.android.sdk.stats.PSDEvent
import com.intellij.openapi.util.ActionCallback
import com.intellij.ui.navigation.History
import com.intellij.ui.navigation.Place

class ModulePropertiesConfigPanel(module: PsAndroidModule, context: PsContext) :
  ConfigPanel<PsAndroidModule>(
    context,
    module.parent,
    module,
    module,
    androidModulePropertiesModel(context, module)
  ),
  ModelPanel<PsAndroidModule> {

  override val title = "Properties"
  override fun setHistory(history: History?) = Unit
  override fun navigateTo(place: Place?, requestFocus: Boolean): ActionCallback = ActionCallback.DONE
  override fun queryPlace(place: Place) = Unit
  override val topConfigurable: PSDEvent.PSDTopTab = PSDEvent.PSDTopTab.PROJECT_STRUCTURE_DIALOG_TOP_TAB_PROPERTIES
}
