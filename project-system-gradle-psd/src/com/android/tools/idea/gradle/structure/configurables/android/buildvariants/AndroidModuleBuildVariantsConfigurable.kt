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
package com.android.tools.idea.gradle.structure.configurables.android.buildvariants

import com.android.tools.idea.gradle.structure.configurables.BasePerspectiveConfigurable
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.configurables.android.buildvariants.buildtypes.BuildTypesConfigurable
import com.android.tools.idea.gradle.structure.configurables.android.buildvariants.productflavors.ProductFlavorsConfigurable
import com.android.tools.idea.gradle.structure.configurables.android.modules.AbstractModuleConfigurable
import com.android.tools.idea.gradle.structure.configurables.createTreeModel
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.BuildVariantsPanel
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

class AndroidModuleBuildVariantsConfigurable(
  context: PsContext,
  perspectiveConfigurable: BasePerspectiveConfigurable,
  module: PsAndroidModule
) : AbstractModuleConfigurable<PsAndroidModule, BuildVariantsPanel>(context, perspectiveConfigurable, module), Disposable {

  private val buildTypesModel = createTreeModel(BuildTypesConfigurable(module, context).also { Disposer.register(this, it) })
  private val productFlavorsModel = createTreeModel(ProductFlavorsConfigurable(module, context).also { Disposer.register(this, it) })

  override fun getId() = "android.psd.build_variants." + displayName
  override fun createPanel() =
      BuildVariantsPanel(context, module, buildTypesModel, productFlavorsModel)

  override fun dispose() = Unit
}


