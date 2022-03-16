// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.android.tools.idea.gradle.structure.configurables.ui.buildvariants

import com.android.tools.idea.gradle.structure.configurables.ConfigurablesTreeModel
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.configurables.ui.AbstractTabbedMainPanel
import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.buildtypes.BuildTypesPanel
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.productflavors.ProductFlavorsPanel
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule

const val BUILD_VARIANTS_PLACE_NAME: String = "android.psd.build_variants"
class BuildVariantsPanel(
    context: PsContext,
    val module: PsAndroidModule,
    buildTypesTreeModel: ConfigurablesTreeModel,
    productFlavorsTreeModel: ConfigurablesTreeModel
) : AbstractTabbedMainPanel(
  context, placeName = BUILD_VARIANTS_PLACE_NAME
) {
  private val buildTypesPanel = BuildTypesPanel(module, buildTypesTreeModel, context.uiSettings)
  private val productFlavorsPanel = ProductFlavorsPanel(module, productFlavorsTreeModel, context.uiSettings)

  init {
    addTab(buildTypesPanel)
    addTab(productFlavorsPanel)
  }

  override fun PsUISettings.getLastSelectedTab(): String? = BUILD_VARIANTS_TAB

  override fun PsUISettings.setLastSelectedTab(value: String) {
    BUILD_VARIANTS_TAB = value
  }
}