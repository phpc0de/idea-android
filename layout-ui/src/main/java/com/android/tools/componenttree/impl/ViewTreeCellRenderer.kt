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
package com.android.tools.componenttree.impl

import com.android.tools.adtui.common.AdtUiUtils
import com.android.tools.adtui.common.ColoredIconGenerator
import com.android.tools.componenttree.api.ViewNodeType
import com.google.common.annotations.VisibleForTesting
import com.intellij.ui.SimpleColoredRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.text.nullize
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.TreeCellRenderer

/**
 * A renderer for an Android View node.
 *
 * In order to allow the text to be fitted in length a 2 step approach is taken:
 *  1: Setup the component with the full text and the unselected icon.
 *     This resulting render component is ideal for measuring the preferred size of the tree node.
 *  2: Before painting perform the following adjustments:
 *      - Adjust the text such that "..." is shown if the text doesn't fit inside the tree width.
 *      - Adjust colors for selection and focus including the generation of a special selected icon.
 */
class ViewTreeCellRenderer<T>(private val type: ViewNodeType<T>) : TreeCellRenderer {
  private val renderer = ColoredViewRenderer()

  // Setup the SimpleColoredRenderer for measuring the preferred size of the node.
  // Store enough of the values specified to make the necessary adjustments if the
  // renderer is used to paint.
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    renderer.reset()
    val node = type.clazz.cast(value) ?: return renderer

    renderer.currentTree = tree as? TreeImpl
    renderer.currentRow = row
    renderer.currentDepth = renderer.currentTree?.model?.computeDepth(value) ?: 1
    renderer.selectedValue = selected
    // the "hasFocus" parameter is wrong when there are multiple selected nodes so check the tree instead:
    renderer.focusedValue = tree.hasFocus() && selected

    renderer.id = stripId(type.idOf(node))
    renderer.tagName = type.tagNameOf(node).substringAfterLast('.')
    renderer.textValue = type.textValueOf(node)
    renderer.treeIcon = type.iconOf(node)
    renderer.enabledValue = type.isEnabled(node)

    renderer.generate()
    return renderer
  }

  /**
   * The renderer component.
   *
   * If you need to update this renderer you probably should update the [computeSearchString] below as well.
   */
  @VisibleForTesting
  class ColoredViewRenderer : SimpleColoredRenderer() {
    var currentTree: TreeImpl? = null
    var currentDepth: Int = 1
    var currentRow = -1
    var selectedValue = false
    var focusedValue = false
    var id: String? = null
    var tagName = ""
    var textValue: String? = null
    var treeIcon: Icon? = null
    var enabledValue = true

    private val baseFontMetrics = getFontMetrics(UIUtil.getLabelFont())
    private val boldFontMetrics = getFontMetrics(deriveFont(UIUtil.getLabelFont(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES))

    // Do not make the SimpleColoredRenderer paint the background
    override fun shouldPaintBackground() = false

    init {
      font = UIUtil.getLabelFont()
      ipad = JBUI.emptyInsets()
    }

    /**
     * Reset all fields.
     */
    fun reset() {
      clear()
      currentTree = null
      currentRow = -1
      id = null
      tagName = ""
      textValue = null
    }

    /**
     * Generate the fragments with full length of the strings.
     */
    fun generate() = generate(0)

    // We are painting, first make the necessary adjustments...
    override fun paintComponent(g: Graphics) {
      if (currentTree != null) {
        adjustForPainting()
      }
      super.paintComponent(g)
    }

    /**
     * Generate the text from className, component ID, the text value.
     *
     * If the ID is available show that first in bold followed by the text value if that is available in gray.
     * If either of these 2 strings are missing then also show the className.
     */
    private fun generate(maxWidth: Int) {
      clear()
      icon = treeIcon
      toolTipText = generateTooltip()
      var attributes = if (!enabledValue) SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES else SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
      if (!append(id, attributes, boldFontMetrics, maxWidth)) {
        return
      }
      if (id == null || textValue.isNullOrEmpty()) {
        val tagText = if (id != null) " - $tagName" else tagName
        attributes = if (!enabledValue) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES
        if (!append(tagText, attributes, baseFontMetrics, maxWidth)) {
          return
        }
      }
      attributes = if (!selectedValue || !enabledValue) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES
      textValue?.nullize()?.let { append(" - \"$it\"", attributes, baseFontMetrics, maxWidth) }
    }

    /**
     * Adjust the renderer in preparation for painting.
     *
     * - adjust the text such that we shrink the text pieces to fit the available width.
     *   Note that if the renderer is going to be used for painting the row current that is
     *   currently expanded by the expandableItemsHandler of the tree then we need the full text.
     * - set the correct foreground & background colors
     * - if painting on a blue background (focus & selected) generate and use a white icon
     */
    @VisibleForTesting
    fun adjustForPainting() {
      val maxWidth = currentTree?.computeMaxRenderWidth(currentDepth) ?: 0
      if (preferredSize.width > maxWidth && currentTree?.isRowCurrentlyExpanded(currentRow) != true) {
        generate(maxWidth)
      }
      foreground = UIUtil.getTreeForeground(selectedValue, focusedValue)
      background = UIUtil.getTreeBackground(selectedValue, focusedValue)
      icon = treeIcon?.let { if (focusedValue) ColoredIconGenerator.generateWhiteIcon(it) else it }
      isTransparentIconBackground = true
    }

    /**
     * Add a fragment of [text] to the renderer using the given text [attributes].
     *
     * If a [maxWidth] is specified the [metrics] is used to measure if there is enough room in the
     * renderer to show this text fragment. If there is not enough room the text is replaced with
     * a string that ends with ellipsis "...".
     * Return true if the text was included unchanged.
     */
    private fun append(text: String?, attributes: SimpleTextAttributes, metrics: FontMetrics, maxWidth: Int): Boolean {
      var actual = text ?: return true
      var unchanged = true
      if (maxWidth > 0) {
        val availableSpace = maxWidth - preferredSize.width
        if (metrics.stringWidth(actual) > availableSpace) {
          actual = AdtUiUtils.shrinkToFit(text, metrics, availableSpace.toFloat())
          unchanged = actual != text
        }
      }
      append(actual, attributes)
      return unchanged
    }

    private fun generateTooltip(): String {
      val text = when {
        id == null -> textValue.orEmpty()
        textValue == null -> id.orEmpty()
        else -> "$id: \"$textValue\""
      }
      if (text.isEmpty()) {
        return tagName
      }
      else {
        return """
        <html>
          $tagName<br/>
          $text
        </html>
        """.trimIndent()
      }
    }

    private fun deriveFont(font: Font, attributes: SimpleTextAttributes): Font {
      if (font.style == attributes.fontStyle && !attributes.isSmaller) {
        return font
      }
      val size = if (attributes.isSmaller) UIUtil.getFontSize(UIUtil.FontSize.SMALL) else font.size2D
      return font.deriveFont(attributes.fontStyle, size)
    }
  }

  companion object {

    /**
     * A SpeedSearch string calculated from the Android View node properties.
     *
     * If you need to update this method you probably should update the [ColoredViewRenderer] above as well.
     */
    fun <T> computeSearchString(type: ViewNodeType<T>, node: T): String {
      val id = type.idOf(node)
      val textValue = type.textValueOf(node)
      var str = stripId(id).orEmpty()
      if (id == null || textValue == null) {
        if (id != null) {
          str += " - "
        }
        str += type.tagNameOf(node).substringAfterLast('.')
      }
      textValue?.let { str += " - \"$it\"" }
      return str
    }

    private fun stripId(id: String?): String? {
      return id?.substringAfter('/')
    }
  }
}
