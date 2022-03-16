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

import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.rendering.RenderSettings
import com.android.tools.idea.uibuilder.api.ViewEditor
import com.android.tools.idea.uibuilder.api.ViewHandler
import com.android.tools.idea.uibuilder.api.actions.ToggleViewAction
import com.android.tools.idea.uibuilder.scene.LayoutlibSceneManager
import com.intellij.util.ui.LafIconLookup

class ToggleAllShowDecorationsAction(label: String = "Show System UI") :
  ToggleViewAction(null, LafIconLookup.getIcon("checkmark"), label, label) {
  override fun isSelected(editor: ViewEditor,
                          handler: ViewHandler,
                          parent: NlComponent,
                          selectedChildren: MutableList<NlComponent>): Boolean {
    val surface = editor.scene.designSurface
    return surface.models
             .map { surface.getSceneManager(it) as? LayoutlibSceneManager }
             .first()
             ?.isShowingDecorations ?: false
  }

  override fun setSelected(editor: ViewEditor,
                           handler: ViewHandler,
                           parent: NlComponent,
                           selectedChildren: MutableList<NlComponent>,
                           selected: Boolean) {
    // Save as global setting
    RenderSettings.getProjectSettings(editor.model.project).showDecorations = selected

    val surface = editor.scene.designSurface
    surface.models
      .mapNotNull { surface.getSceneManager(it) as? LayoutlibSceneManager }
      .forEach { it.setShowDecorations(selected) }
    surface.requestRender()
  }

}
