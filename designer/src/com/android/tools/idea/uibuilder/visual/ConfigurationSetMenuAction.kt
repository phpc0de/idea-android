/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.tools.adtui.actions.DropDownAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import icons.StudioIcons


/**
 * The dropdown action used to choose the configuration set in visualization tool.
 */
class ConfigurationSetMenuAction(private val listener: ConfigurationSetListener,
                                 defaultSet: ConfigurationSet)
  : DropDownAction(null, "Configuration Set", null) {

  private var currentConfigurationSet = defaultSet

  init {
    var isPreviousGroupEmpty = true
    for (group in ConfigurationSetProvider.getGroupedConfigurationSets()) {
      if (!isPreviousGroupEmpty) {
        addSeparator()
      }
      val groupItems = group.filter { it.visible }
      groupItems.forEach { add(SetConfigurationSetAction(it)) }
      isPreviousGroupEmpty = groupItems.isEmpty()
    }
  }

  override fun displayTextInToolbar() = true

  override fun update(e: AnActionEvent) {
    updatePresentation(e.presentation)
  }

  private fun updatePresentation(presentation: Presentation) {
    presentation.text = currentConfigurationSet.name
  }

  private fun selectConfigurationSet(newSet: ConfigurationSet) {
    if (newSet !== currentConfigurationSet) {
      currentConfigurationSet = newSet
      updatePresentation(templatePresentation)
      listener.onSelectedConfigurationSetChanged(newSet)
      getChildren(null).mapNotNull { it as? SetConfigurationSetAction }.forEach { it.updatePresentation() }
    }
  }

  private inner class SetConfigurationSetAction(private val configurationSet: ConfigurationSet)
    : AnAction(configurationSet.name,
               "Set configuration set to ${configurationSet.name}",
               if (currentConfigurationSet === configurationSet) StudioIcons.Common.CHECKED else null) {

    override fun actionPerformed(e: AnActionEvent) {
      selectConfigurationSet(configurationSet)
    }

    fun updatePresentation() {
      templatePresentation.icon = if (currentConfigurationSet === configurationSet) StudioIcons.Common.CHECKED else null
    }
  }
}
