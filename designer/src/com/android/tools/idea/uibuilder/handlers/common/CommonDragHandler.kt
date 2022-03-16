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
package com.android.tools.idea.uibuilder.handlers.common

import com.android.tools.idea.common.model.AndroidCoordinate
import com.android.tools.idea.common.model.AndroidDpCoordinate
import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.common.scene.SceneComponent
import com.android.tools.idea.common.scene.TemporarySceneComponent
import com.android.tools.idea.common.scene.target.Target
import com.android.tools.idea.uibuilder.api.DragHandler
import com.android.tools.idea.common.api.DragType
import com.android.tools.idea.common.api.InsertType
import com.android.tools.idea.common.scene.SceneContext
import com.android.tools.idea.common.scene.target.CommonDragTarget
import com.android.tools.idea.uibuilder.api.ViewEditor
import com.android.tools.idea.uibuilder.api.ViewGroupHandler
import com.android.tools.idea.uibuilder.handlers.DelegatingViewGroupHandler
import com.android.tools.idea.uibuilder.handlers.TabLayoutHandler
import com.android.tools.idea.uibuilder.handlers.preference.PreferenceCategoryHandler
import com.android.tools.idea.uibuilder.handlers.preference.PreferenceScreenHandler
import com.android.tools.idea.uibuilder.menu.ItemHandler
import com.android.tools.idea.uibuilder.menu.MenuHandler
import com.android.tools.idea.uibuilder.model.h
import com.android.tools.idea.uibuilder.model.w

private const val ERROR_UNDEFINED = "undefined"

/**
 * [DragHandler] handles the dragging from Palette and ComponentTree for all Layouts.
 */
internal class CommonDragHandler(editor: ViewEditor,
                                 handler: ViewGroupHandler,
                                 layout: SceneComponent,
                                 components: List<NlComponent>,
                                 type: DragType
) : DragHandler(editor, handler, layout, components, type) {

  private val dragTarget: CommonDragTarget?

  init {
    val dragged = components[0]
    val component = layout.scene.getSceneComponent(dragged) ?: TemporarySceneComponent(layout.scene, dragged).apply {
      setSize(editor.pxToDp(dragged.w), editor.pxToDp(dragged.h))
    }

    dragTarget = CommonDragTarget(component, fromToolWindow = true)

    component.setTargetProvider { _ -> mutableListOf<Target>(dragTarget) }
    component.updateTargets()
    // Note: Don't use [dragged] in this lambda function since the content of components may be replaced within interaction.
    // This weird implementation may be fixed in the future, but we just work around here.
    component.setComponentProvider { _ -> components[0] }
    layout.addChild(component)
    component.drawState = SceneComponent.DrawState.DRAG
  }

  override fun start(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, modifiers: Int) {
    if (dragTarget == null) {
      return
    }
    super.start(x, y, modifiers)
    dragTarget.mouseDown(x, y)
  }

  override fun update(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, modifiers: Int, sceneContext: SceneContext): String? {
    if (dragTarget == null) {
      return ERROR_UNDEFINED
    }
    val result = super.update(x, y, modifiers, sceneContext)
    dragTarget.mouseDrag(x, y, emptyList(), sceneContext)
    dragTarget.component.scene.requestLayoutIfNeeded()
    return result
  }

  // Note that coordinate is AndroidCoordinate, not AndroidDpCoordinate.
  override fun commit(@AndroidCoordinate x: Int, @AndroidCoordinate y: Int, modifiers: Int,
                      insertType: InsertType) {
    if (dragTarget == null) {
      return
    }
    dragTarget.insertType = insertType
    @AndroidDpCoordinate val dx = editor.pxToDp(x)
    @AndroidDpCoordinate val dy = editor.pxToDp(y)
    dragTarget.mouseRelease(dx, dy, emptyList())

    // Remove Temporary SceneComponent
    val component = dragTarget.component
    if (component is TemporarySceneComponent) {
      layout.scene.removeComponent(component)
    }
    component.drawState = SceneComponent.DrawState.NORMAL
    layout.scene.requestLayoutIfNeeded()
  }

  override fun cancel() {
    if (dragTarget == null) {
      return
    }
    if (dragTarget.component is TemporarySceneComponent) {
      layout.scene.removeComponent(dragTarget.component)
    }
    dragTarget.component.drawState = SceneComponent.DrawState.NORMAL
    dragTarget.mouseCancel()
  }

  companion object {
    /**
     * The classes of [ViewGroupHandler] which don't support [CommonDragHandler] yet.
     * TODO: makes [CommonDragHandler] can be used in all [ViewGroupHandler].
     */
    private val HANDLER_CLASSES_NOT_SUPPORT= listOf(
      ItemHandler::class,
      MenuHandler::class,
      PreferenceCategoryHandler::class,
      PreferenceScreenHandler::class,
      TabLayoutHandler::class
    )

    @JvmStatic
    fun isSupportCommonDragHandler(handler: ViewGroupHandler): Boolean {
      var checkedHandler = handler
      while (checkedHandler is DelegatingViewGroupHandler) {
        checkedHandler = checkedHandler.delegateHandler
      }
      return HANDLER_CLASSES_NOT_SUPPORT.none { it.isInstance(checkedHandler) }
    }
  }
}
