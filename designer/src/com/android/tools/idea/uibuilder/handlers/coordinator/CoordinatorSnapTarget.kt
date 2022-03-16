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

import com.android.SdkConstants
import com.android.tools.idea.common.model.AndroidDpCoordinate
import com.android.tools.idea.common.model.AttributesTransaction
import com.android.tools.idea.common.model.NlAttributesHolder
import com.android.tools.idea.common.scene.NonPlaceholderDragTarget
import com.android.tools.idea.common.scene.SceneContext
import com.android.tools.idea.common.scene.draw.DisplayList
import com.android.tools.idea.common.scene.target.BaseTarget
import com.android.tools.idea.common.scene.target.Target
import com.android.tools.idea.uibuilder.model.ensureLiveId
import java.awt.Color

/**
 * A "snap" target for CoordinatorLayout. When a CoordinatorDragTarget
 * is moved close to this, we'll snap the component at the location.
 */
class CoordinatorSnapTarget constructor(type: Type) : BaseTarget(), NonPlaceholderDragTarget {
  private val DEBUG: Boolean = false

  enum class Type {
    LEFT, LEFT_TOP, LEFT_BOTTOM,
    RIGHT, RIGHT_TOP, RIGHT_BOTTOM,
    TOP, CENTER, BOTTOM
  }

  private var myType: Type = type
  private var mySize: Int = 32

  override fun getPreferenceLevel(): Int = Target.ANCHOR_LEVEL

  override fun layout(context: SceneContext, left: Int, top: Int, right: Int, bottom: Int): Boolean {
    var l = left
    var t = top
    var r = right
    var b = bottom
    val d = Math.min(Math.min(b- t, r- l) / 4, mySize)
    if (myType == Type.RIGHT_TOP || myType == Type.RIGHT || myType == Type.RIGHT_BOTTOM) {
      l = r - d
    } else if (myType == Type.TOP || myType == Type.CENTER || myType == Type.BOTTOM) {
      l = l + (r - l) / 2 - d / 2
    }
    if (myType == Type.LEFT_BOTTOM || myType == Type.BOTTOM || myType == Type.RIGHT_BOTTOM) {
      t = b - d
    } else if (myType == Type.LEFT || myType == Type.CENTER || myType == Type.RIGHT) {
      t = t + (b - t) / 2 - d / 2
    }
    r = l + d
    b = t + d
    myLeft = l.toFloat()
    myTop = t.toFloat()
    myRight = r.toFloat()
    myBottom = b.toFloat()
    return false
  }

  override fun render(list: DisplayList, sceneContext: SceneContext) {
    @Suppress("ConstantConditionIf")
    if (DEBUG) {
      val color = if (mIsOver) Color.orange else Color.green
      list.addRect(sceneContext, myLeft, myTop, myRight, myBottom, color)
      list.addLine(sceneContext, myLeft, myTop, myRight, myBottom, color)
      list.addLine(sceneContext, myLeft, myBottom, myRight, myTop, color)
    }
    DrawSnapTarget.add(list, sceneContext, myLeft, myTop, myRight, myBottom, mIsOver)
  }

  override fun isHittable(): Boolean {
    return !myComponent.isSelected && !myComponent.isDragging;
  }

  fun isSnapped(@AndroidDpCoordinate x: Int, @AndroidDpCoordinate y: Int) =
    coordinateInRange(x, myLeft, myRight) && coordinateInRange(y, myTop, myBottom)

  private fun coordinateInRange(x: Int, left: Float, right: Float): Boolean = x >= left && x <= right

  fun snap(attributes: NlAttributesHolder) {
    val value = when (myType) {
      Type.LEFT -> "start|center"
      Type.RIGHT -> "end|center"
      Type.TOP -> "top|center"
      Type.BOTTOM -> "bottom|center"
      Type.LEFT_TOP -> "start|top"
      Type.LEFT_BOTTOM -> "start|bottom"
      Type.CENTER -> "center"
      Type.RIGHT_TOP -> "end|top"
      Type.RIGHT_BOTTOM -> "end|bottom"
    }
    attributes.setAttribute(SdkConstants.AUTO_URI, SdkConstants.ATTR_LAYOUT_ANCHOR_GRAVITY, value)
    attributes.setAttribute(SdkConstants.AUTO_URI, SdkConstants.ATTR_LAYOUT_ANCHOR, SdkConstants.NEW_ID_PREFIX + myComponent.nlComponent.ensureLiveId())
  }
}
