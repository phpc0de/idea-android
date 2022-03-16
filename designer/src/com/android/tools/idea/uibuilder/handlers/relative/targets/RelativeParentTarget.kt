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
package com.android.tools.idea.uibuilder.handlers.relative.targets

import com.android.SdkConstants
import com.android.tools.idea.common.scene.SceneComponent
import com.android.tools.idea.common.scene.SceneContext
import com.android.tools.idea.common.scene.draw.DisplayList
import com.android.tools.idea.common.scene.draw.DrawRegion
import com.android.tools.idea.common.scene.target.Target
import com.android.tools.idea.uibuilder.scene.target.Notch.Horizontal
import com.android.tools.idea.uibuilder.scene.target.Notch
import com.android.tools.idea.uibuilder.scene.target.Notch.Vertical
import com.google.common.collect.ImmutableList
import com.intellij.ui.JBColor
import java.awt.BasicStroke
import java.awt.Graphics2D

private const val DEBUG = false
private const val NOTCH_GAP_SIZE = 10

private val DRAW_LINE_STROKE = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)

/**
 * Reusable actions.
 */
private val ACTION_MAP = mapOf(
    RelativeParentTarget.Type.LEFT to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_START, "true") },
    RelativeParentTarget.Type.TOP to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP, "true") },
    RelativeParentTarget.Type.RIGHT to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_END, "true") },
    RelativeParentTarget.Type.BOTTOM to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM, "true") },
    RelativeParentTarget.Type.CENTER_HORIZONTAL to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_CENTER_HORIZONTAL, "true") },
    RelativeParentTarget.Type.CENTER_VERTICAL to Notch.Action { it.setAndroidAttribute(SdkConstants.ATTR_LAYOUT_CENTER_VERTICAL, "true") }
)

/**
 * The Target for providing Notches of RelativeLayout.
 */
class RelativeParentTarget(val type: Type) : BaseRelativeTarget() {

  enum class Type { LEFT, TOP, RIGHT, BOTTOM, CENTER_HORIZONTAL, CENTER_VERTICAL }

  private var x1 = Int.MIN_VALUE
  private var y1 = Int.MIN_VALUE
  private var x2 = Int.MIN_VALUE
  private var y2 = Int.MIN_VALUE

  override fun getPreferenceLevel(): Int = Target.GUIDELINE_ANCHOR_LEVEL

  override fun layout(context: SceneContext, l: Int, t: Int, r: Int, b: Int): Boolean {
    x1 = myComponent.drawX
    y1 = myComponent.drawY
    x2 = myComponent.drawX + myComponent.drawWidth
    y2 = myComponent.drawY + myComponent.drawHeight

    when (type) {
      Type.LEFT -> x2 = x1
      Type.TOP -> y2 = y1
      Type.RIGHT -> x1 = x2
      Type.BOTTOM -> y1 = y2
      Type.CENTER_HORIZONTAL -> {
        val cx = (x1 + x2) / 2
        x1 = cx
        x2 = cx
      }
      Type.CENTER_VERTICAL -> {
        val cy = (y1 + y2) / 2
        y1 = cy
        y2 = cy
      }
    }
    return false
  }

  override fun render(list: DisplayList, sceneContext: SceneContext) {
    if (myIsHighlight) {
      list.add(DrawLine(sceneContext.getSwingXDip(x1.toFloat()), sceneContext.getSwingYDip(y1.toFloat()),
          sceneContext.getSwingXDip(x2.toFloat()), sceneContext.getSwingYDip(y2.toFloat())))
    }
    @Suppress("ConstantConditionIf")
    if (DEBUG) {
      drawDebug(list, sceneContext)
    }
  }

  /**
   * Draw the debug graphics
   */
  private fun drawDebug(list: DisplayList, sceneContext: SceneContext) =
      list.addRect(sceneContext, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), if (myIsHighlight) JBColor.GREEN else JBColor.RED)

  override fun fill(owner: SceneComponent, snappableComponent: SceneComponent, notchBuilder: ImmutableList.Builder<Notch>) {
    val notch = when (type) {
      RelativeParentTarget.Type.LEFT -> Horizontal(owner, owner.drawX, owner.drawX)
      RelativeParentTarget.Type.TOP -> Vertical(owner, owner.drawY, owner.drawY)
      RelativeParentTarget.Type.RIGHT -> Horizontal(
        owner, owner.drawX + owner.drawWidth - snappableComponent.drawWidth, owner.drawX + owner.drawWidth
      )
      RelativeParentTarget.Type.BOTTOM -> Vertical(
        owner, owner.drawY + owner.drawHeight - snappableComponent.drawHeight, owner.drawY + owner.drawHeight
      )
      RelativeParentTarget.Type.CENTER_HORIZONTAL -> Horizontal(
        owner, owner.drawX + (owner.drawWidth - snappableComponent.drawWidth) / 2, owner.drawX + owner.drawWidth / 2
      )
      RelativeParentTarget.Type.CENTER_VERTICAL -> Vertical(
        owner, owner.drawY + (owner.drawHeight - snappableComponent.drawHeight) / 2, owner.drawY + owner.drawHeight / 2
      )
    }
    notchBuilder.add(notch)

    notch.setAction(ACTION_MAP[type])
    notch.setGap(NOTCH_GAP_SIZE)
    notch.target = this
  }

  private class DrawLine(val x1: Int, val y1: Int, val x2: Int, val y2: Int) : DrawRegion() {
    override fun paint(g: Graphics2D, sceneContext: SceneContext) {
      val originalStroke = g.stroke

      g.color = sceneContext.colorSet.dragReceiverFrames
      g.stroke = DRAW_LINE_STROKE
      g.drawLine(x1, y1, x2, y2)

      g.stroke = originalStroke
    }
  }
}
