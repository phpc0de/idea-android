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
package com.android.tools.idea.uibuilder.visual

import com.android.resources.ScreenOrientation
import com.android.sdklib.devices.Device
import com.android.tools.idea.common.model.NlModel
import com.android.tools.idea.common.type.typeOf
import com.android.tools.idea.configurations.ConfigurationManager
import com.android.tools.idea.configurations.ConfigurationMatcher
import com.android.tools.idea.uibuilder.model.NlComponentHelper
import com.android.tools.idea.uibuilder.type.LayoutFileType
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import org.jetbrains.android.facet.AndroidFacet
import java.util.ArrayList

/**
 * We predefined some pixel devices for now.
 */
@VisibleForTesting
val TABLETS_TO_DISPLAY = listOf("Nexus 7", "Nexus 9", "Nexus 10")

/**
 * This class provides the [NlModel]s with predefined pixel devices for [VisualizationForm].
 */
object TabletModelsProvider: VisualizationModelsProvider {

  @VisibleForTesting
  val deviceCaches = mutableMapOf<ConfigurationManager, List<Device>>()

  override fun createNlModels(parentDisposable: Disposable, file: PsiFile, facet: AndroidFacet): List<NlModel> {
    if (file.typeOf() != LayoutFileType) {
      return emptyList()
    }

    val virtualFile = file.virtualFile ?: return emptyList()

    val configurationManager = ConfigurationManager.getOrCreateInstance(facet)
    val tablets = deviceCaches.getOrElse(configurationManager) {
      val deviceList = ArrayList<Device>()
      for (name in TABLETS_TO_DISPLAY) {
        configurationManager.devices.firstOrNull { device -> name == device.displayName }?.let { deviceList.add(it) }
      }
      deviceCaches[configurationManager] = deviceList
      Disposer.register(configurationManager) { deviceCaches.remove(configurationManager) }
      deviceList
    }

    assert(tablets.isNotEmpty())

    val models = mutableListOf<NlModel>()
    val defaultConfig = configurationManager.getConfiguration(virtualFile)
    val orientations = listOf(ScreenOrientation.PORTRAIT, ScreenOrientation.LANDSCAPE)

    for (device in tablets) {
      for (orientation in orientations) {
        val config = defaultConfig.clone()
        config.setEffectiveDevice(device, device.getState(orientation.shortDisplayValue))
        var label = device.displayName
        val size = device.getScreenSize(orientation)
        if (size != null) {
          label = label + " (" + size.width + " x " + size.height + ")"
        }
        val betterFile = ConfigurationMatcher.getBetterMatch(config, null, null, null, null) ?: virtualFile
        models.add(NlModel.builder(facet, betterFile, config)
                     .withParentDisposable(parentDisposable)
                     .withModelDisplayName(label)
                     .withComponentRegistrar { NlComponentHelper.registerComponent(it) }
                     .build())
      }
    }
    return models
  }
}
