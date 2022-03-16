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
package com.android.tools.idea.layoutinspector.ui

import com.android.tools.idea.layoutinspector.metrics.statistics.SessionStatistics
import com.android.tools.idea.layoutinspector.model.DrawViewNode
import com.android.tools.idea.layoutinspector.model.InspectorModel
import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient
import com.android.tools.idea.layoutinspector.tree.TreeSettings
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.DataKey
import java.awt.Image
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

val DEVICE_VIEW_MODEL_KEY = DataKey.create<DeviceViewPanelModel>(DeviceViewPanelModel::class.qualifiedName!!)

data class ViewDrawInfo(
  val bounds: Shape,
  val transform: AffineTransform,
  val node: DrawViewNode,
  val hitLevel: Int,
  val isCollapsed: Boolean
)

private data class LevelListItem(val node: DrawViewNode, val isCollapsed: Boolean)

class DeviceViewPanelModel(
  private val model: InspectorModel,
  private val stats: SessionStatistics,
  val treeSettings: TreeSettings,
  private val client: (() -> InspectorClient?)? = null
) {
  @VisibleForTesting
  var xOff = 0.0
  @VisibleForTesting
  var yOff = 0.0

  private var rootBounds: Rectangle = Rectangle()
  private var maxDepth: Int = 0

  internal val maxWidth
    get() = hypot((maxDepth * layerSpacing).toFloat(), rootBounds.width.toFloat()).toInt()

  internal val maxHeight
    get() = hypot((maxDepth * layerSpacing).toFloat(), rootBounds.height.toFloat()).toInt()

  val isRotated
    get() = xOff != 0.0 || yOff != 0.0

  @VisibleForTesting
  var hitRects = listOf<ViewDrawInfo>()

  val modificationListeners = mutableListOf<() -> Unit>()

  var overlay: Image? = null
    set(value) {
      if (value != null) {
        resetRotation()
      }
      field = value
      modificationListeners.forEach { it() }
    }

  var overlayAlpha: Float = INITIAL_ALPHA_PERCENT / 100f
    set(value) {
      field = value
      modificationListeners.forEach { it() }
    }

  var layerSpacing: Int = INITIAL_LAYER_SPACING
    set(value) {
      field = value
      refresh()
    }

  init {
    model.modificationListeners.add { _, new, _ ->
      if (new == null) {
        overlay = null
      }
      if (client?.invoke()?.capabilities?.contains(InspectorClient.Capability.SUPPORTS_SKP) != true) {
        resetRotation()
      }
    }
    refresh()
  }

  val isActive
    get() = !model.isEmpty

  /**
   * Find all the views drawn under the given point, in order from closest to farthest from the front, except
   * if the view is an image drawn by a parent at a different depth, the depth of the parent is used rather than
   * the depth of the child.
   */
  fun findViewsAt(x: Double, y: Double): Sequence<ViewNode> =
    hitRects.asReversed()
      .asSequence()
      .filter { it.bounds.contains(x, y) }
      .sortedByDescending { it.hitLevel }
      .mapNotNull { it.node.findFilteredOwner(treeSettings) }
      .distinct()

  fun findTopViewAt(x: Double, y: Double): ViewNode? = findViewsAt(x, y).firstOrNull()

  fun rotate(xRotation: Double, yRotation: Double) {
    xOff = (xOff + xRotation).coerceIn(-1.0, 1.0)
    yOff = (yOff + yRotation).coerceIn(-1.0, 1.0)
    refresh()
  }

  fun refresh() {
    if (xOff == 0.0 && yOff == 0.0) {
      stats.rotation.toggledTo2D()
    }
    else {
      stats.rotation.toggledTo3D()
    }
    if (model.isEmpty) {
      rootBounds = Rectangle()
      maxDepth = 0
      hitRects = emptyList()
      modificationListeners.forEach { it() }
      return
    }
    val root = model.root

    val levelLists = mutableListOf<MutableList<LevelListItem>>()
    // Each window should start completely above the previous window, hence level = levelLists.size
    ViewNode.readDrawChildren { drawChildren ->
      root.drawChildren().forEach { buildLevelLists(it, levelLists, levelLists.size, levelLists.size, drawChildren) }
    }
    maxDepth = levelLists.size

    val newHitRects = mutableListOf<ViewDrawInfo>()
    val transform = AffineTransform()
    var magnitude = 0.0
    var angle = 0.0
    if (maxDepth > 0) {
      rootBounds = levelLists[0].map { it.node.bounds.bounds }.reduce { acc, bounds -> acc.apply { add(bounds) } }
      root.x = rootBounds.x
      root.y = rootBounds.x
      root.width = rootBounds.width
      root.height = rootBounds.height
      transform.translate(-rootBounds.width / 2.0, -rootBounds.height / 2.0)

      // Don't allow rotation to completely edge-on, since some rendering can have problems in that situation. See issue 158452416.
      // You might say that this is ( •_•)>⌐■-■ / (⌐■_■) an edge-case.
      magnitude = min(0.98, hypot(xOff, yOff))
      angle = if (abs(xOff) < 0.00001) PI / 2.0 else atan(yOff / xOff)

      transform.translate(rootBounds.width / 2.0 - rootBounds.x, rootBounds.height / 2.0 - rootBounds.y)
      transform.rotate(angle)
    }
    else {
      rootBounds = Rectangle()
    }
    rebuildRectsForLevel(transform, magnitude, angle, levelLists, newHitRects)
    hitRects = newHitRects.toList()
    modificationListeners.forEach { it() }
  }

  private fun buildLevelLists(node: DrawViewNode,
                              levelListCollector: MutableList<MutableList<LevelListItem>>,
                              minLevel: Int,
                              previousLevel: Int,
                              drawChildren: ViewNode.() -> List<DrawViewNode>) {
    var newLevelIndex = minLevel
    val owner = node.findFilteredOwner(treeSettings)
    if (owner == null || model.isVisible(owner)) {
      // Starting from the highest level and going down, find the first level where something intersects with this view. We'll put this view
      // in the next level above that (that is, the last level, starting from the top, where there's space).
      val rootArea = Area(node.bounds)
      // find the last level where this node intersects with what's at that level already.
      newLevelIndex = levelListCollector
        .subList(minLevel, levelListCollector.size)
        .indexOfLast { it.map { (node, _) -> Area(node.bounds) }.any { a -> a.run { intersect(rootArea); !isEmpty } } }

      var shouldDraw = true
      var isCollapsed = false
      // If we can collapse, merge into the layer we found if it's the same as our parent node's layer
      if (node.canCollapse(treeSettings) && newLevelIndex <= previousLevel &&
          (levelListCollector.getOrNull(previousLevel)?.any {
            it.node.findFilteredOwner(treeSettings) == node.findFilteredOwner(treeSettings)
          } == true || (newLevelIndex == -1 && node.findFilteredOwner(treeSettings) == null))) {
        isCollapsed = true
        shouldDraw = node.drawWhenCollapsed
        if (newLevelIndex == -1) {
          // We didn't find anything to merge into, so just draw at the bottom.
          newLevelIndex = 0
        }
      }
      else {
        // We're not collapsing, so draw in the level above the last level with overlapping contents
        newLevelIndex++
      }
      // The list index we got was from a sublist starting at minLevel, so we have to add minLevel back in.
      newLevelIndex += minLevel

      if (shouldDraw) {
        val levelList = levelListCollector.getOrElse(newLevelIndex) {
          mutableListOf<LevelListItem>().also { levelListCollector.add(it) }
        }
        levelList.add(LevelListItem(node, isCollapsed))
      }
    }
    for (drawChild in node.children(drawChildren)) {
      buildLevelLists(drawChild, levelListCollector, 0, newLevelIndex, drawChildren)
    }
  }

  private fun rebuildRectsForLevel(
    transform: AffineTransform,
    magnitude: Double,
    angle: Double,
    allLevels: List<List<LevelListItem>>,
    newHitRects: MutableList<ViewDrawInfo>
  ) {
    val ownerToLevel = mutableMapOf<ViewNode?, Int>()

    allLevels.forEachIndexed { level, levelList ->
      levelList.forEach { (view, isCollapsed) ->
        val hitLevel = ownerToLevel.getOrPut(view.findFilteredOwner(treeSettings)) { level }
        val viewTransform = AffineTransform(transform)

        val sign = if (xOff < 0) -1 else 1
        viewTransform.translate(magnitude * (level - maxDepth / 2) * layerSpacing * sign, 0.0)
        viewTransform.scale(sqrt(1.0 - magnitude * magnitude), 1.0)
        viewTransform.rotate(-angle)
        viewTransform.translate(-rootBounds.width / 2.0, -rootBounds.height / 2.0)

        val rect = viewTransform.createTransformedShape(view.unfilteredOwner.transformedBounds)
        newHitRects.add(ViewDrawInfo(rect, viewTransform, view, hitLevel, isCollapsed))
      }
    }
  }

  fun resetRotation() {
    xOff = 0.0
    yOff = 0.0
    refresh()
  }

  /**
   * Fire the modification listeners manually.
   */
  fun fireModified() = modificationListeners.forEach { it() }
}