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
package com.android.tools.idea.layoutinspector.properties

import com.android.tools.property.panel.api.ControlTypeProvider
import com.android.tools.property.panel.api.EnumSupportProvider
import com.android.tools.property.panel.api.FilteredPTableModel
import com.android.tools.property.panel.api.FilteredPTableModel.PTableModelFactory.create
import com.android.tools.property.panel.api.InspectorBuilder
import com.android.tools.property.panel.api.InspectorPanel
import com.android.tools.property.panel.api.PropertiesTable
import com.android.tools.property.panel.api.TableUIProvider
import com.android.tools.property.ptable2.PTableItem

class InspectorTableBuilder(
  private val title: String,
  private val filter: (InspectorPropertyItem) -> Boolean,
  private val model: InspectorPropertiesModel,
  enumSupportProvider: EnumSupportProvider<InspectorPropertyItem>,
  controlTypeProvider: ControlTypeProvider<InspectorPropertyItem>,
  private val itemComparator: Comparator<PTableItem> = FilteredPTableModel.alphabeticalSortOrder,
  private val searchable: Boolean = false
) : InspectorBuilder<InspectorPropertyItem> {

  private val editorProvider = ResolutionStackEditorProvider(model, enumSupportProvider, controlTypeProvider)
  // TODO: Check if a reified type parameter can be used instead of the explicit class
  private val uiProvider = TableUIProvider.create(InspectorPropertyItem::class.java, controlTypeProvider, editorProvider)

  override fun attachToInspector(inspector: InspectorPanel, properties: PropertiesTable<InspectorPropertyItem>) {
    val tableModel = create(model, filter, {}, itemComparator, valueEditable = { editorProvider.isValueEditable(it) })
    if (tableModel.items.isEmpty()) {
      return
    }
    val titleModel = inspector.addExpandableTitle(title, true)
    inspector.addTable(tableModel, searchable, uiProvider, emptyList(), titleModel)
  }
}
