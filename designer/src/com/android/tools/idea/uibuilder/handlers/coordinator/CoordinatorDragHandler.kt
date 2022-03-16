/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.handlers.coordinator

import com.android.tools.idea.common.api.DragType
import com.android.tools.idea.common.api.InsertType
import com.android.tools.idea.common.model.AndroidCoordinate
import com.android.tools.idea.common.model.AndroidDpCoordinate
import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.uibuilder.api.*
import com.android.tools.idea.uibuilder.model.*
import com.android.tools.idea.common.scene.SceneComponent
import com.android.tools.idea.common.scene.SceneContext
import com.android.tools.idea.common.scene.TemporarySceneComponent
import com.intellij.openapi.diagnostic.Logger

/**
 * CoordinatorLayout drag handler
 */
class CoordinatorDragHandler(editor: ViewEditor, handler: ViewGroupHandler,
                             layout: SceneComponent,
                             components: List<NlComponent>,
                             type: DragType
) : DragHandler(editor, handler, layout, components, type) {
  private var sceneComponent: SceneComponent
  private val dragTarget = CoordinatorDragTarget()
  private val snapTargets = mutableListOf<CoordinatorSnapTarget>()

  init {
    assert(components.size == 1)
    val dragged = components[0]
    sceneComponent = layout.scene.getSceneComponent(dragged) ?:
        TemporarySceneComponent(layout.scene, dragged).apply { setSize(editor.pxToDp(dragged.w), editor.pxToDp(dragged.h)) }

    sceneComponent.setTargetProvider { listOf(dragTarget) }
    sceneComponent.updateTargets()
    dragTarget.component = sceneComponent
    sceneComponent.isSelected = true

    sceneComponent.drawState = SceneComponent.DrawState.DRAG
    layout.addChild(sceneComponent)
  }

  override fun start(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, modifiers: Int) {
    super.start(x, y, modifiers)
    dragTarget.mouseDown(x, y)

    snapTargets.clear()
    snapTargets.addAll(sceneComponent.parent?.children?.
        map({ it.targets })?.flatten()?.filterIsInstance<CoordinatorSnapTarget>()?.toList() ?: emptyList())
  }

  override fun update(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, modifiers: Int, sceneContext: SceneContext): String? {
    val ret = super.update(x, y, modifiers, sceneContext)

    @AndroidDpCoordinate val dx = x + startX - sceneComponent.drawWidth / 2
    @AndroidDpCoordinate val dy = y + startY - sceneComponent.drawHeight / 2

    dragTarget.mouseDrag(dx, dy, snapTargets.filter({ it.isSnapped(x, y) }), sceneContext)
    return ret
  }

  override fun commit(@AndroidCoordinate x: Int, @AndroidCoordinate y: Int, modifiers: Int,
                      insertType: InsertType) {
    editor.insertChildren(layout.nlComponent, components, -1, insertType)

    when (insertType) {
      InsertType.CREATE -> dragWidgetFromPalette(x, y)
      InsertType.MOVE -> dragWidgetFromComponentTree(x, y)
      else -> Logger.getInstance(javaClass.name).error("Unexpected InsertType in ${javaClass.name}#commit}")
    }

    layout.scene.removeComponent(sceneComponent)
    layout.scene.requestLayoutIfNeeded()
  }

  private fun dragWidgetFromPalette(@AndroidCoordinate x: Int, @AndroidCoordinate y: Int) {
    layout.scene.needsRebuildList()
    @AndroidDpCoordinate val dx = editor.pxToDp(x) + startX - sceneComponent.drawWidth / 2
    @AndroidDpCoordinate val dy = editor.pxToDp(y) + startY - sceneComponent.drawHeight / 2
    for (child in components) {
      dragTarget.mouseRelease(dx, dy, child)
    }
  }

  private fun dragWidgetFromComponentTree(@AndroidCoordinate x: Int, @AndroidCoordinate y: Int) {
    for (child in components) {
      val sceneComponent = layout.getSceneComponent(child) ?: continue
      sceneComponent.isDragging = true
      dragTarget.component = sceneComponent
      @AndroidDpCoordinate val dx = editor.pxToDp(x) + startX - sceneComponent.drawWidth / 2
      @AndroidDpCoordinate val dy = editor.pxToDp(y) + startY - sceneComponent.drawHeight / 2
      dragTarget.mouseRelease(dx, dy, emptyList())
    }
  }

  override fun cancel() {
    editor.scene.removeComponent(sceneComponent)
    dragTarget.mouseCancel()
  }
}
