/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.idea

import com.android.tools.idea.gradle.project.facet.ndk.NdkFacet
import com.android.tools.idea.gradle.project.model.V1NdkModel
import com.android.tools.idea.gradle.project.sync.IdeAndroidNativeVariantsModels

class IdeAndroidNativeVariantsModelsWrapper(val moduleId: String, val variants: IdeAndroidNativeVariantsModels) {
  fun mergeInto(ndkFacet: NdkFacet) {
    val ndkModuleModel = ndkFacet.ndkModuleModel ?: return

    fun copyAndMerge(v1NdkModel: V1NdkModel): V1NdkModel =
      v1NdkModel.copy(nativeVariantAbis = (variants.v1NativeVariantAbis.orEmpty() + v1NdkModel.nativeVariantAbis).distinctBy { it.abi })

    // NOTE: V2 models do not contain synced variants themselves and instead variant details are loaded from files generated by build.
    (ndkModuleModel.ndkModel as? V1NdkModel)
      ?.let(::copyAndMerge)
      ?.let { ndkFacet.setNdkModuleModel(ndkModuleModel.copy(ndkModel = it)) }
  }
}
