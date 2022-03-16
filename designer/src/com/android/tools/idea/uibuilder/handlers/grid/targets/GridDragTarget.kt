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
package com.android.tools.idea.uibuilder.handlers.grid.targets

import com.android.SdkConstants
import com.android.tools.idea.common.model.AndroidDpCoordinate
import com.android.tools.idea.common.model.AttributesTransaction
import com.android.tools.idea.common.scene.NonPlaceholderDragTarget
import com.android.tools.idea.common.scene.Scene
import com.android.tools.idea.common.scene.SceneContext
import com.android.tools.idea.common.scene.draw.DisplayList
import com.android.tools.idea.common.scene.draw.DrawCommand
import com.android.tools.idea.common.scene.target.BaseTarget
import com.android.tools.idea.common.scene.target.Target
import com.android.tools.idea.uibuilder.handlers.grid.GridBarriers
import com.android.tools.idea.uibuilder.handlers.grid.getGridBarriers
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.text.StringUtil
import org.intellij.lang.annotations.JdkConstants
import java.awt.Cursor
import java.awt.Graphics2D

private const val MIN_WIDTH = 16
private const val MIN_HEIGHT = 16

/**
 * Target to handle the drag of GridLayout's children
 * FIXME: lock the grid decoration when dragging widget.
 */
class GridDragTarget(isSupportLibrary: Boolean) : BaseTarget(), NonPlaceholderDragTarget {

  private val nameSpace = if (isSupportLibrary) SdkConstants.AUTO_URI else SdkConstants.ANDROID_URI

  private var barrier: GridBarriers? = null
  private var selectedRow = -1
  private var selectedColumn = -1

  private var firstMouseX = 0
  private var firstMouseY = 0
  private var offsetX = 0
  private var offsetY = 0

  override fun layout(context: SceneContext, l: Int, t: Int, r: Int, b: Int): Boolean {
    val xShift = if (r - l < MIN_WIDTH) (MIN_WIDTH - (r - l)) / 2 else 0
    myLeft = (l - xShift).toFloat()
    myRight = (r + xShift).toFloat()

    val yShift = if (b - t < MIN_HEIGHT) (MIN_HEIGHT - (b - t)) / 2 else 0
    myTop = (t - yShift).toFloat()
    myBottom = (b + yShift).toFloat()

    if (!myComponent.isDragging) {
      // don't update grid when dragging component
      val parent = myComponent.parent ?: return false
      barrier = getGridBarriers(parent)
    }

    return false
  }

  override fun render(list: DisplayList, sceneContext: SceneContext) {
    if (myComponent.isDragging && barrier != null) {
      val b = barrier as GridBarriers
      list.add(DrawDraggableRegionCommand(b.left, b.top, b.right, b.bottom))

      if (selectedColumn != -1 && selectedRow != -1) {
        val bounds = b.getBounds(selectedRow, selectedColumn) ?: return
        list.add(DragCellCommand(bounds.x, bounds.y, bounds.width, bounds.height))
      }
    }
  }

  override fun mouseDown(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int) {
    firstMouseX = x
    firstMouseY = y
    offsetX = x - myComponent.getDrawX(System.currentTimeMillis())
    offsetY = y - myComponent.getDrawY(System.currentTimeMillis())
  }

  override fun mouseDrag(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, closestTargets: List<Target>, ignored: SceneContext) {
    myComponent.isDragging = true

    val parent = myComponent.parent ?: return

    val dragX = x - offsetX
    val dragY = y - offsetY

    val componentX = Math.max(parent.drawX, Math.min(dragX, parent.drawX + parent.drawWidth - myComponent.drawWidth))
    val componentY = Math.max(parent.drawY, Math.min(dragY, parent.drawY + parent.drawHeight - myComponent.drawHeight))
    myComponent.setPosition(componentX, componentY)

    val component = myComponent.nlComponent
    val attributes = component.startAttributeTransaction()
    updateAttributes(attributes, x, y)
    attributes.apply()
    component.fireLiveChangeEvent()
    myComponent.scene.repaint()
  }

  private fun updateAttributes(attributes: AttributesTransaction, @AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int) {
    if (barrier == null) return
    val b = barrier as GridBarriers
    selectedColumn = b.getColumnAtX(x)
    selectedRow = b.getRowAtY(y)

    if (selectedColumn != -1) {
      attributes.setAttribute(nameSpace, SdkConstants.ATTR_LAYOUT_COLUMN, selectedColumn.toString())
    }
    if (selectedRow != -1) {
      attributes.setAttribute(nameSpace, SdkConstants.ATTR_LAYOUT_ROW, selectedRow.toString())
    }
  }

  override fun mouseRelease(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int, closestTarget: List<Target>) {
    if (!myComponent.isDragging) return

    myComponent.isDragging = false

    val component = myComponent.authoritativeNlComponent

    val attributes = component.startAttributeTransaction()
    updateAttributes(attributes, x, y)
    attributes.apply()

    if (!(Math.abs(x - firstMouseX) <= 1 && Math.abs(y - firstMouseY) <= 1)) {
      WriteCommandAction
          .writeCommandAction(component.model.file)
          .withName("Dragged " + StringUtil.getShortName(component.tagName))
          .run<Throwable> { attributes.commit() }
    }

    myComponent.updateTargets()

    myComponent.scene.markNeedsLayout(Scene.IMMEDIATE_LAYOUT)
    myComponent.scene.repaint()
  }

  override fun isHittable() = !myComponent.isDragging

  override fun getPreferenceLevel() = Target.DRAG_LEVEL

  override fun getMouseCursor(@JdkConstants.InputEventMask modifiersEx: Int): Cursor? = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

  override fun canChangeSelection() = true
}

private class DrawDraggableRegionCommand(@AndroidDpCoordinate val x1: Int,
                                         @AndroidDpCoordinate val y1: Int,
                                         @AndroidDpCoordinate val x2: Int,
                                         @AndroidDpCoordinate val y2: Int)
  : DrawCommand {

  override fun getLevel() = DrawCommand.CLIP_LEVEL

  override fun paint(g: Graphics2D, sceneContext: SceneContext) {
    val swingX = sceneContext.getSwingXDip(x1.toFloat())
    val swingY = sceneContext.getSwingYDip(y1.toFloat())
    val width = sceneContext.getSwingDimensionDip((x2 - x1).toFloat())
    val height = sceneContext.getSwingDimensionDip((y2 - y1).toFloat())
    g.color = sceneContext.colorSet.dragReceiverBackground
    g.fillRect(swingX, swingY, width, height)
  }

  override fun serialize() = "${javaClass.name}:($x1, $y1) - ($x2, $y2)"
}

private class DragCellCommand(@AndroidDpCoordinate val x: Int,
                              @AndroidDpCoordinate val y: Int,
                              @AndroidDpCoordinate val w: Int,
                              @AndroidDpCoordinate val h: Int)
  : DrawCommand {

  override fun getLevel() = DrawCommand.CLIP_LEVEL

  override fun paint(g: Graphics2D, sceneContext: SceneContext) {
    val swingX = sceneContext.getSwingXDip(x.toFloat())
    val swingY = sceneContext.getSwingYDip(y.toFloat())
    val width = sceneContext.getSwingDimensionDip(w.toFloat())
    val height = sceneContext.getSwingDimensionDip(h.toFloat())
    g.color = sceneContext.colorSet.dragReceiverFrames
    g.fillRect(swingX, swingY, width, height)
  }

  override fun serialize() = "${javaClass.name},$x,$y,$w,$h"
}
