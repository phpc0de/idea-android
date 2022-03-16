/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.surface.layout

import com.android.tools.adtui.common.SwingCoordinate
import java.awt.Dimension
import kotlin.math.max

/**
 * A [SurfaceLayoutManager] which layouts all [PositionableContent]s vertically or horizontally depending on the given available size.
 *
 * The [horizontalPadding] and [verticalPadding] are the minimum gaps between [PositionableContent] and the edges of surface.
 * The [horizontalViewDelta] and [verticalViewDelta] are the fixed gap between different [PositionableContent]s.
 * Padding and view delta are always the same physical sizes on screen regardless the zoom level.
 *
 * [startBorderAlignment] allows to modify the start border aligment. See [Alignment].
 */
open class SingleDirectionLayoutManager(@SwingCoordinate private val horizontalPadding: Int,
                                        @SwingCoordinate private val verticalPadding: Int,
                                        @SwingCoordinate private val horizontalViewDelta: Int,
                                        @SwingCoordinate private val verticalViewDelta: Int,
                                        private val startBorderAlignment: Alignment = Alignment.CENTER)
  : SurfaceLayoutManager {

  /**
   * Determines the alignment for the start border of the element.
   * For a vertical alignment, [START] would mean left and [END] right. For horizontal, [START] means top and [END] means bottom.
   */
  enum class Alignment {
    START,
    CENTER,
    END
  }

  private var previousHorizontalPadding = 0
  private var previousVerticalPadding = 0

  override fun getPreferredSize(content: Collection<PositionableContent>,
                                availableWidth: Int,
                                availableHeight: Int,
                                dimension: Dimension?)
    : Dimension {
    val dim = dimension ?: Dimension()

    val vertical = isVertical(content, availableWidth, availableHeight)

    val preferredWidth: Int
    val preferredHeight: Int
    if (vertical) {
      preferredWidth = content.maxOf { contentSize.width } ?: 0
      preferredHeight = content.sumOf { margin.vertical + contentSize.height + verticalViewDelta } - verticalViewDelta
    }
    else {
      preferredWidth = content.sumOf { margin.horizontal + contentSize.width + horizontalViewDelta } - horizontalViewDelta
      preferredHeight = content.maxOf { contentSize.height } ?: 0
    }

    val width = max(0, preferredWidth)
    val height = max(0, preferredHeight)
    dim.setSize(width, height)
    return dim
  }

  override fun getRequiredSize(content: Collection<PositionableContent>, availableWidth: Int, availableHeight: Int, dimension: Dimension?): Dimension {
    val dim = dimension ?: Dimension()

    val requiredWidth: Int
    val requiredHeight: Int
    if (isVertical(content, availableWidth, availableHeight)) {
      requiredWidth = content.maxOf { scaledContentSize.width } ?: 0
      requiredHeight = content.sumOf { margin.vertical + scaledContentSize.height + verticalViewDelta } - verticalViewDelta
    }
    else {
      requiredWidth = content.sumOf { margin.horizontal + scaledContentSize.width + horizontalViewDelta } - horizontalViewDelta
      requiredHeight = content.maxOf { scaledContentSize.height } ?: 0
    }

    val width = max(0, requiredWidth)
    val height = max(0, requiredHeight)
    dim.setSize(width, height)
    return dim
  }

  protected open fun isVertical(content: Collection<PositionableContent>, availableWidth: Int, availableHeight: Int): Boolean {
    if (content.isEmpty()) {
      return false
    }

    val primary = content.sortByPosition().first()
    return (availableHeight > 3 * availableWidth / 2) || primary.scaledContentSize.width > primary.scaledContentSize.height
  }

  override fun layout(content: Collection<PositionableContent>, availableWidth: Int, availableHeight: Int, keepPreviousPadding: Boolean) {
    if (content.isEmpty()) {
      return
    }

    val vertical = isVertical(content, availableWidth, availableHeight)

    val startX: Int
    val startY: Int
    if (keepPreviousPadding) {
      startX = previousHorizontalPadding
      startY = previousVerticalPadding
    } else {
      val requiredSize = getRequiredSize(content, availableWidth, availableHeight, null)
      val requiredWidth = requiredSize.width
      val requiredHeight = requiredSize.height
      startX = max((availableWidth - requiredWidth) / 2, horizontalPadding)
      startY = max((availableHeight - requiredHeight) / 2, verticalPadding)
      previousHorizontalPadding = startX
      previousVerticalPadding = startY
    }

    if (vertical) {
      var nextY = startY
      for (sceneView in content) {
        nextY += sceneView.margin.top
        val xPosition = max(startX, when (startBorderAlignment) {
          Alignment.START -> startX
          Alignment.END -> availableWidth - sceneView.scaledContentSize.width
          Alignment.CENTER -> (availableWidth - sceneView.scaledContentSize.width) / 2
        })
        sceneView.setLocation(xPosition, nextY)
        nextY += sceneView.scaledContentSize.height + sceneView.margin.bottom + verticalViewDelta
      }
    }
    else {
      var nextX = startX
      for (sceneView in content) {
        // Centered in the horizontal centerline
        val yPosition = max(startY, when (startBorderAlignment) {
          Alignment.START -> startY
          Alignment.END -> availableHeight - (sceneView.scaledContentSize.height + sceneView.margin.vertical)
          Alignment.CENTER -> availableHeight / 2 - (sceneView.scaledContentSize.height + sceneView.margin.vertical) / 2
        })
        sceneView.setLocation(nextX, yPosition)
        nextX += sceneView.scaledContentSize.width + horizontalViewDelta
      }
    }
  }
}

/**
 * [SingleDirectionLayoutManager] that forces the content to always be vertical.
 */
class VerticalOnlyLayoutManager(@SwingCoordinate horizontalPadding: Int,
                                @SwingCoordinate verticalPadding: Int,
                                @SwingCoordinate horizontalViewDelta: Int,
                                @SwingCoordinate verticalViewDelta: Int,
                                val startBorderAlignment: Alignment) : SingleDirectionLayoutManager(
  horizontalPadding, verticalPadding, horizontalViewDelta, verticalViewDelta, startBorderAlignment) {
  override fun isVertical(content: Collection<PositionableContent>, availableWidth: Int, availableHeight: Int): Boolean = true
}

// Helper functions to improve readability
private fun Collection<PositionableContent>.sumOf(mapFunc: PositionableContent.() -> Int) = map(mapFunc).sum()
private fun Collection<PositionableContent>.maxOf(mapFunc: PositionableContent.() -> Int) = map(mapFunc).maxOrNull()
