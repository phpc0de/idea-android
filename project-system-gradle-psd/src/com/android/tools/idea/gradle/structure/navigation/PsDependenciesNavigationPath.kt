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
package com.android.tools.idea.gradle.structure.navigation

import com.android.tools.idea.gradle.structure.configurables.DependenciesPerspectiveConfigurable
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.model.PsModulePath
import com.android.tools.idea.gradle.structure.model.PsPlaceBasedPath
import com.android.tools.idea.structure.dialog.ProjectStructureConfigurable
import com.intellij.ui.navigation.Place

data class PsDependenciesNavigationPath(override val parent: PsModulePath) : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    val mainConfigurable = context.mainConfigurable
    val target = mainConfigurable.findConfigurable(
      DependenciesPerspectiveConfigurable::class.java)!!

    ProjectStructureConfigurable.putPath(place, target)
    target.putNavigationPath(place, parent.gradlePath)
  }

  override val canHide: Boolean get() = true

  override fun toString(): String = "Dependencies"
}