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
package com.android.tools.idea.uibuilder.actions

import com.android.tools.idea.common.surface.DesignSurface
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SelectNextAction(private val surface: DesignSurface) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val selectable = surface.selectableComponents
    if (selectable.isEmpty()) {
      return
    }

    val selectionModel = surface.selectionModel
    val selection = selectionModel.selection

    val next = if (selection.size == 1) {
      val index = selectable.indexOf(selection[0])
      selectable[(index + 1) % selectable.size]
    }
    else {
      selectable.first()
    }

    selectionModel.setSelection(listOf(next))
    surface.repaint()
  }
}
