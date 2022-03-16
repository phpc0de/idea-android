/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.android.dependencies.project.treeview

import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractPsModelNode
import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractPsNode
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule
import com.intellij.openapi.roots.ui.CellAppearanceEx
import com.intellij.openapi.util.text.StringUtil.isEmpty
import com.intellij.ui.HtmlListCellRenderer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES
import com.intellij.ui.treeStructure.SimpleNode

class TargetAndroidModuleNode internal constructor(
  parent: AbstractPsNode,
  module: PsAndroidModule,
  private val version: String?,
  private val children: List<AbstractPsModelNode<*>>
) : AbstractPsModelNode<PsAndroidModule>(parent, parent.uiSettings), CellAppearanceEx {

  override val models: List<PsAndroidModule> = listOf(module)

  init {
    autoExpandNode = true
    updateNameAndIcon()
  }

  override fun getChildren(): Array<SimpleNode> = children.toTypedArray()

  override fun getText(): String = myName

  override fun customize(component: SimpleColoredComponent) {
    if (!isEmpty(version)) {
      component.append(" ")
      component.append("($version)", GRAY_ATTRIBUTES)
    }
  }
}
