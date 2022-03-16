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
package com.android.tools.idea.common.surface

import com.android.annotations.concurrency.UiThread
import com.android.tools.adtui.common.SwingCoordinate
import com.android.tools.idea.common.model.scaleBy
import com.android.tools.idea.common.surface.layout.findAllScanlines
import com.android.tools.idea.common.surface.layout.findLargerScanline
import com.android.tools.idea.common.surface.layout.findSmallerScanline
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.uibuilder.surface.layout.PositionableContent
import com.android.tools.idea.uibuilder.surface.layout.PositionableContentLayoutManager
import com.android.tools.idea.uibuilder.surface.layout.horizontal
import com.google.common.annotations.VisibleForTesting
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max


/**
 * Distance between the bottom bound of model name and top bound of SceneView.
 */
@SwingCoordinate
private const val TOP_BAR_BOTTOM_MARGIN = 3

/**
 * Distance between the top bound of bottom bar and bottom bound of SceneView.
 */
@SwingCoordinate
private const val BOTTOM_BAR_TOP_MARGIN = 3

/**
 * Minimum allowed width for the SceneViewPeerPanel.
 */
@SwingCoordinate
private const val SCENE_VIEW_PEER_PANEL_MIN_WIDTH = 100

/**
 * Minimum allowed width for the model name label.
 */
@SwingCoordinate
private const val MODEL_NAME_LABEL_MIN_WIDTH = 20

/**
 * A [PositionableContentLayoutManager] for a [DesignSurface] with only one [PositionableContent].
 */
class SinglePositionableContentLayoutManager : PositionableContentLayoutManager() {
  override fun layoutContainer(content: Collection<PositionableContent>, availableSize: Dimension) {
    content.singleOrNull()?.setLocation(0, 0)
  }

  override fun preferredLayoutSize(content: Collection<PositionableContent>, availableSize: Dimension): Dimension =
    content
      .singleOrNull()
      ?.getScaledContentSize(null)
    ?: availableSize
}

private data class LayoutData private constructor(
  val scale: Double,
  val modelName: String?,
  val x: Int,
  val y: Int,
  val scaledSize: Dimension) {

  // Used to avoid extra allocations in isValidFor calls
  private val cachedDimension = Dimension()

  /**
   * Returns whether this [LayoutData] is still valid (has not changed) for the given [SceneView]
   */
  fun isValidFor(sceneView: SceneView): Boolean =
    scale == sceneView.scale &&
    x == sceneView.x && y == sceneView.y &&
    modelName == sceneView.scene.sceneManager.model.modelDisplayName &&
    scaledSize == sceneView.getContentSize(cachedDimension).scaleBy(sceneView.scale)

  companion object {
    fun fromSceneView(sceneView: SceneView): LayoutData =
      LayoutData(
        sceneView.scale,
        sceneView.scene.sceneManager.model.modelDisplayName,
        sceneView.x,
        sceneView.y,
        sceneView.getContentSize(null).scaleBy(sceneView.scale))
  }
}

/**
 * A Swing component associated to the given [SceneView]. There will be one of this components in the [DesignSurface]
 * per every [SceneView] available. This panel will be positioned on the coordinates of the [SceneView] and can be
 * used to paint Swing elements on top of the [SceneView].
 */
@VisibleForTesting
class SceneViewPeerPanel(val sceneView: SceneView,
                         private val sceneViewToolbar: JComponent?,
                         private val sceneViewBottomBar: JComponent?,
                         private val sceneViewLeftBar: JComponent?) : JPanel() {
  /**
   * Contains cached layout data that can be used by this panel to verify when it's been invalidated
   * without having to explicitly call [revalidate]
   */
  private var layoutData = LayoutData.fromSceneView(sceneView)

  private val cachedContentSize = Dimension()
  private val cachedScaledContentSize = Dimension()
  private val cachedPreferredSize = Dimension()

  /**
   * This label displays the [SceneView] model if there is any
   */
  private val modelNameLabel = JBLabel().apply {
    maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
    isEnabled = false
  }

  val positionableAdapter = object : PositionableContent() {
    override val x: Int get() = sceneView.x
    override val y: Int get() = sceneView.y

    override val margin: Insets
      get() {
        val contentSize = getScaledContentSize(null)
        val sceneViewMargin = sceneView.margin.also {
          // Extend top to account for the top toolbar
          it.top += sceneViewTopPanel.preferredSize.height
          it.bottom += sceneViewBottomPanel.preferredSize.height
          it.left += sceneViewLeftPanel.preferredSize.width
        }
        return if (contentSize.width < minimumSize.width ||
                   contentSize.height < minimumSize.height) {
          // If there is no content, or the content is smaller than the minimum size, pad the margins to occupy the empty space.
          // Horizontally, we align the content to the left.
          val hSpace = (minimumSize.width - contentSize.width).coerceAtLeast(0)
          val vSpace = (minimumSize.height - contentSize.height).coerceAtLeast(0)

          val (left, right) = when (alignmentX) {
            LEFT_ALIGNMENT -> sceneViewMargin.left to sceneViewMargin.right + hSpace
            RIGHT_ALIGNMENT -> sceneViewMargin.left + hSpace to sceneViewMargin.right
            CENTER_ALIGNMENT -> sceneViewMargin.left + hSpace / 2 to sceneViewMargin.right + hSpace / 2
            else -> throw IllegalArgumentException("$alignmentX is not supported")
          }

          Insets(sceneViewMargin.top,
                 left,
                 sceneViewMargin.bottom + vSpace,
                 right)
        }
        else {
          sceneViewMargin
        }
      }

    override fun getContentSize(dimension: Dimension?): Dimension = if (sceneView.hasContentSize())
      sceneView.getContentSize(dimension).also {
        cachedContentSize.size = it
      }
    else
      dimension?.apply {
        size = cachedContentSize
      } ?: Dimension(cachedContentSize)

    /**
     * Returns the current size of the view content, excluding margins. This is the same as {@link #getContentSize()} but accounts for the
     * current zoom level
     *
     * @param dimension optional existing {@link Dimension} instance to be reused. If not null, the values will be set and this instance
     *                  returned.
     */
    override fun getScaledContentSize(dimension: Dimension?): Dimension {
      val outputDimension = dimension ?: Dimension()

      return getContentSize(outputDimension).scaleBy(sceneView.scale)
    }


    /**
     * Applies the calculated coordinates from this adapter to the backing SceneView.
     */
    private fun applyLayout() {
      getScaledContentSize(cachedScaledContentSize)
      val margin = margin // To avoid recalculating the size
      setBounds(x - margin.left,
                y - margin.top,
                cachedScaledContentSize.width + margin.left + margin.right,
                cachedScaledContentSize.height + margin.top + margin.bottom)
      sceneView.scene.needsRebuildList()
    }

    override fun setLocation(x: Int, y: Int) {
      // The SceneView is painted right below the top toolbar panel
      sceneView.setLocation(x, y)

      // After positioning the view, we re-apply the bounds to the SceneViewPanel.
      // We do this even if x & y did not change since the size might have.
      applyLayout()
    }
  }

  /**
   * This panel wraps both the label and the toolbar and puts them left aligned (label) and right
   * aligned (the toolbar).
   */
  @VisibleForTesting
  val sceneViewTopPanel = JPanel(BorderLayout()).apply {
    border = JBUI.Borders.emptyBottom(TOP_BAR_BOTTOM_MARGIN)
    isOpaque = false
    add(modelNameLabel, BorderLayout.CENTER)
    if (sceneViewToolbar != null) {
      add(sceneViewToolbar, BorderLayout.LINE_END)
    }
    // The space of name label is sacrified when there is no enough width to display the toolbar.
    // When it happens, the label will be trimmed and show the ellipsis at its tail.
    // User can still hover it to see the full label in the tooltips.
    val minWidth = MODEL_NAME_LABEL_MIN_WIDTH + (sceneViewToolbar?.minimumSize?.width ?: 0)
    minimumSize = Dimension(minWidth, minimumSize.height)
  }

  val sceneViewBottomPanel = JPanel(BorderLayout()).apply {
    border = JBUI.Borders.emptyTop(BOTTOM_BAR_TOP_MARGIN)
    isOpaque = false
    isVisible = true
    if (sceneViewBottomBar != null) {
      add(sceneViewBottomBar, BorderLayout.CENTER)
    }
  }

  val sceneViewLeftPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    isVisible = true
    if (sceneViewLeftBar != null) {
      add(sceneViewLeftBar, BorderLayout.CENTER)
    }
  }


  init {
    isOpaque = false
    layout = null

    add(sceneViewTopPanel)
    add(sceneViewBottomPanel)
    add(sceneViewLeftPanel)
    // This setup the initial positions of sceneViewTopPanel, sceneViewBottomPanel, and sceneViewLeftPanel.
    // Otherwise they are all placed at top-left corner before first time layout.
    doLayout()
  }

  override fun isValid(): Boolean {
    return super.isValid() && layoutData.isValidFor(sceneView)
  }

  override fun doLayout() {
    layoutData = LayoutData.fromSceneView(sceneView)

    // If there is a model name, we manually assign the content of the modelNameLabel and position it here.
    // Once this panel gets more functionality, we will need the use of a layout manager. For now, we just lay out the component manually.
    if (layoutData.modelName == null) {
      modelNameLabel.text = ""
      modelNameLabel.toolTipText = ""
      sceneViewTopPanel.isVisible = false
    }
    else {
      modelNameLabel.text = layoutData.modelName
      modelNameLabel.toolTipText = layoutData.modelName
      // We layout the top panel. We make the width to match the SceneViewPanel width and we let it choose its own
      // height.
      sceneViewTopPanel.setBounds(0, 0,
                                  width + insets.horizontal,
                                  sceneViewTopPanel.preferredSize.height)
      sceneViewTopPanel.isVisible = true
    }
    sceneViewBottomPanel.setBounds(0, sceneViewTopPanel.preferredSize.height + positionableAdapter.scaledContentSize.height, width + insets.horizontal, sceneViewBottomPanel.preferredSize.height)
    sceneViewLeftPanel.setBounds(0, sceneViewTopPanel.preferredSize.height, sceneViewLeftPanel.preferredSize.width, height)
    super.doLayout()
  }

  /** [Dimension] used to avoid extra allocations calculating [getPreferredSize] */
  override fun getPreferredSize(): Dimension = positionableAdapter.getScaledContentSize(cachedPreferredSize).also {
    it.width = it.width + positionableAdapter.margin.left + positionableAdapter.margin.right
    it.height = it.height + positionableAdapter.margin.top + positionableAdapter.margin.bottom
  }

  override fun getMinimumSize(): Dimension =
    Dimension(
      max(sceneViewTopPanel.minimumSize.width, SCENE_VIEW_PEER_PANEL_MIN_WIDTH),
      sceneViewBottomPanel.preferredSize.height + sceneViewTopPanel.minimumSize.height + JBUI.scale(20))
}

/**
 * A [JPanel] responsible for displaying [SceneView]s provided by the [sceneViewProvider].
 *
 * @param interactionLayersProvider A [Layer] provider that returns the additional interaction [Layer]s, if any
 * @param layoutManager the [PositionableContentLayoutManager] responsible for positioning and measuring the [SceneView]s
 */
internal class SceneViewPanel(private val sceneViewProvider: () -> Collection<SceneView>,
                              private val interactionLayersProvider: () -> Collection<Layer>,
                              layoutManager: PositionableContentLayoutManager) :
  JPanel(layoutManager) {
  /**
   * Alignment for the {@link SceneView} when its size is less than the minimum size.
   * If the size of the {@link SceneView} is less than the minimum, this enum describes how to align the content within
   * the rectangle formed by the minimum size.
   */
  var sceneViewAlignment: Float = CENTER_ALIGNMENT
    set(value) {
      if (value != field) {
        field = value
        components
          .filterIsInstance<SceneViewPeerPanel>()
          .forEach { it.alignmentX = value }
        repaint()
      }
    }

  /**
   * Returns the components of this panel that are [PositionableContent]
   */
  val positionableContent: Collection<PositionableContent>
    get() = components.filterIsInstance<SceneViewPeerPanel>()
      .map { it.positionableAdapter }
      .toList()

  @UiThread
  private fun revalidateSceneViews() {
    // Check if the SceneViews are still valid
    val designSurfaceSceneViews = sceneViewProvider()
    val currentSceneViews = findSceneViews()

    if (designSurfaceSceneViews == currentSceneViews) return // No updates

    // Invalidate the current components
    removeAll()
    designSurfaceSceneViews.forEachIndexed { index, sceneView ->
      val toolbar = if (StudioFlags.NELE_SCENEVIEW_TOP_TOOLBAR.get()) {
        sceneView.surface.actionManager.getSceneViewContextToolbar(sceneView)
      }
      else {
        null
      }

      val bottomBar = if(StudioFlags.NELE_SCENEVIEW_BOTTOM_BAR.get()) {
        sceneView.surface.actionManager.getSceneViewBottomBar(sceneView)
      } else {
        null
      }

      // The left bar is only added for the first panel
      val leftBar = if(StudioFlags.NELE_SCENEVIEW_LEFT_BAR.get() && index == 0) {
        sceneView.surface.actionManager.getSceneViewLeftBar(sceneView)
      } else {
        null
      }

      add(SceneViewPeerPanel(sceneView, toolbar, bottomBar, leftBar).also {
        it.alignmentX = sceneViewAlignment
      })
    }
  }

  override fun doLayout() {
    revalidateSceneViews()
    super.doLayout()
  }

  override fun paintComponent(graphics: Graphics) {
    super.paintComponent(graphics)
    val sceneViewPeerPanels = components.filterIsInstance<SceneViewPeerPanel>()

    if (sceneViewPeerPanels.isEmpty()) {
      return
    }

    val g2d = graphics.create() as Graphics2D
    try {
      // The visible area in the editor
      val viewportBounds: Rectangle = g2d.clipBounds

      // A Dimension used to avoid reallocating new objects just to obtain the PositionableContent dimensions
      val reusableDimension = Dimension()
      val positionables: Collection<PositionableContent> = sceneViewPeerPanels.map { it.positionableAdapter }
      val horizontalTopScanLines = positionables.findAllScanlines { it.y }
      val horizontalBottomScanLines = positionables.findAllScanlines { it.y + it.getScaledContentSize(reusableDimension).height }
      val verticalLeftScanLines = positionables.findAllScanlines { it.x }
      val verticalRightScanLines = positionables.findAllScanlines { it.x + it.getScaledContentSize(reusableDimension).width }
      @SwingCoordinate val viewportRight = viewportBounds.x + viewportBounds.width
      @SwingCoordinate val viewportBottom = viewportBounds.y + viewportBounds.height
      val clipBounds = Rectangle()
      for (sceneViewPeerPanel in sceneViewPeerPanels) {
        val positionable = sceneViewPeerPanel.positionableAdapter
        val size = positionable.getScaledContentSize(reusableDimension)
        @SwingCoordinate val right = positionable.x + size.width
        @SwingCoordinate val bottom = positionable.y + size.height
        // This finds the maximum allowed area for the screen views to paint into. See more details in the
        // ScanlineUtils.kt documentation.
        @SwingCoordinate var minX = findSmallerScanline(verticalRightScanLines, positionable.x, viewportBounds.x)
        @SwingCoordinate var minY = findSmallerScanline(horizontalBottomScanLines, positionable.y, viewportBounds.y)
        @SwingCoordinate var maxX = findLargerScanline(verticalLeftScanLines,
                                                       right,
                                                       viewportRight)
        @SwingCoordinate var maxY = findLargerScanline(horizontalTopScanLines,
                                                       bottom,
                                                       viewportBottom)

        // Now, (minX, minY) (maxX, maxY) describes the box that a PositionableContent could paint into without painting
        // on top of another PositionableContent render. We use this box to paint the components that are outside of the
        // rendering area.
        // However, now we need to avoid there "out of bounds" components from being on top of each other.
        // To do that, we simply find the middle point, except on the corners of the surface. For example, the
        // first PositionableContent on the left, does not have any other PositionableContent that could paint on its left side so we
        // do not need to find the middle point in those cases.
        minX = if (minX > viewportBounds.x) (minX + positionable.x) / 2 else viewportBounds.x
        maxX = if (maxX < viewportRight) (maxX + right) / 2 else viewportRight
        minY = if (minY > viewportBounds.y) (minY + positionable.y) / 2 else viewportBounds.y
        maxY = if (maxY < viewportBottom) (maxY + bottom) / 2 else viewportBottom
        clipBounds.setBounds(minX, minY, maxX - minX, maxY - minY)
        g2d.clip = clipBounds
        sceneViewPeerPanel.sceneView.paint(g2d)
      }

      val interactionLayers = interactionLayersProvider()
      if (interactionLayers.isNotEmpty()) {
        // Temporary overlays do not have a clipping area
        g2d.clip = viewportBounds

        // Temporary overlays:
        interactionLayersProvider()
          .filter { it.isVisible }
          .forEach { it.paint(g2d) }
      }
    }
    finally {
      g2d.dispose()
    }
  }

  private fun findSceneViews(): List<SceneView> =
    components
      .filterIsInstance<SceneViewPeerPanel>()
      .map { it.sceneView }
      .toList()


  fun findSceneViewRectangle(sceneView: SceneView): Rectangle? =
    components
      .filterIsInstance<SceneViewPeerPanel>()
      .filter { sceneView == it.sceneView }
      .map { it.bounds }
      .firstOrNull()
}