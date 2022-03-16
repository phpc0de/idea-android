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

import com.android.tools.idea.gradle.structure.configurables.BuildVariantsPerspectiveConfigurable
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.configurables.ui.PROPERTY_PLACE_NAME
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.BUILD_VARIANTS_PLACE_NAME
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.buildtypes.BUILD_TYPES_DISPLAY_NAME
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.buildtypes.BUILD_TYPES_PLACE_NAME
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.productflavors.PRODUCT_FLAVORS_DISPLAY_NAME
import com.android.tools.idea.gradle.structure.configurables.ui.buildvariants.productflavors.PRODUCT_FLAVORS_PLACE_NAME
import com.android.tools.idea.gradle.structure.model.PsModulePath
import com.android.tools.idea.gradle.structure.model.PsPlaceBasedPath
import com.android.tools.idea.gradle.structure.model.android.PsBuildType
import com.android.tools.idea.gradle.structure.model.android.PsProductFlavor
import com.android.tools.idea.gradle.structure.model.meta.ModelProperty
import com.android.tools.idea.structure.dialog.ProjectStructureConfigurable.Companion.putPath
import com.intellij.ui.navigation.Place

data class PsBuildVariantsNavigationPath(override val parent: PsModulePath) : PsPlaceBasedPath() {

  override fun queryPlace(place: Place, context: PsContext) {
    val mainConfigurable = context.mainConfigurable
    val target = mainConfigurable.findConfigurable(BuildVariantsPerspectiveConfigurable::class.java)!!

    putPath(place, target)
    target.putNavigationPath(place, parent.gradlePath)
  }

  override val canHide: Boolean = true

  override fun toString(): String = "Build Variants"
}

data class PsBuildTypesNavigationPath(override val parent: PsBuildVariantsNavigationPath) : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(BUILD_VARIANTS_PLACE_NAME, BUILD_TYPES_DISPLAY_NAME)
  }

  override fun toString(): String = "Build Types"
}

data class PsProductFlavorsNavigationPath(override val parent: PsBuildVariantsNavigationPath) : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(BUILD_VARIANTS_PLACE_NAME, PRODUCT_FLAVORS_DISPLAY_NAME)
  }

  override fun toString(): String = "Product Flavors"
}

data class PsBuildTypeNavigationPath(override val parent: PsBuildTypesNavigationPath, val buildTypeName: String) : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(BUILD_TYPES_PLACE_NAME, buildTypeName)
  }

  override fun toString(): String = buildTypeName
  fun property(property: ModelProperty<PsBuildType, *, *, *>): PsPlaceBasedPath = PsBuildTypePropertyNavigationPath(this, property)
}

data class PsProductFlavorNavigationPath(override val parent: PsProductFlavorsNavigationPath, val productFlavorName: String)
  : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(PRODUCT_FLAVORS_PLACE_NAME, productFlavorName)
  }

  override fun toString(): String = productFlavorName
  fun property(property: ModelProperty<PsProductFlavor, *, *, *>): PsPlaceBasedPath = PsProductFlavorPropertyNavigationPath(this, property)
}

data class PsFlavorDimensionNavigationPath(override val parent: PsProductFlavorsNavigationPath, val flavorDimensionName: String)
  : PsPlaceBasedPath() {
  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(PRODUCT_FLAVORS_PLACE_NAME, flavorDimensionName)
  }

  override fun toString(): String = flavorDimensionName
}

data class PsBuildTypePropertyNavigationPath(override val parent: PsBuildTypeNavigationPath, val property: String)
  : PsPlaceBasedPath() {
  constructor(parent: PsBuildTypeNavigationPath, property: ModelProperty<PsBuildType, *, *, *>) : this(parent, property.description)

  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(PROPERTY_PLACE_NAME, property)
  }

  override fun toString(): String = property
}

data class PsProductFlavorPropertyNavigationPath(override val parent: PsProductFlavorNavigationPath, val property: String)
  : PsPlaceBasedPath() {
  constructor(parent: PsProductFlavorNavigationPath, property: ModelProperty<PsProductFlavor, *, *, *>) : this(parent, property.description)

  override fun queryPlace(place: Place, context: PsContext) {
    parent.queryPlace(place, context)
    place.putPath(PROPERTY_PLACE_NAME, property)
  }

  override fun toString(): String = property
}