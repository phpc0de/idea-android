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
package com.android.tools.idea.npw.template.components

import com.android.tools.idea.observable.AbstractProperty
import com.android.tools.idea.observable.ui.SelectedItemProperty
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import org.jetbrains.android.util.AndroidBundle.message
import javax.swing.DefaultComboBoxModel

/**
 * Provides a combobox which presents the user with a list of modules
 */
class ModuleComboProvider : ComponentProvider<ComboBox<Module>>() {
  override fun createComponent(): ComboBox<Module> = ComboBox<Module>(DefaultComboBoxModel()).apply {
    renderer = SimpleListCellRenderer.create { label: JBLabel, module: Module?, _: Int ->
      if (module == null) {
        label.text = message("android.wizard.module.config.new.base.missing")
      }
      else {
        label.icon = ModuleType.get(module).icon
        label.text = module.name
      }
    }
  }

  override fun createProperty(component: ComboBox<Module>): AbstractProperty<*> = SelectedItemProperty<String>(component)
}

