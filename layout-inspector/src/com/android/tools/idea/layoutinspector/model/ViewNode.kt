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
package com.android.tools.idea.layoutinspector.model

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.tree.TreeSettings
import com.android.tools.idea.layoutinspector.tree.TreeViewNode
import com.google.common.annotations.VisibleForTesting
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlTag
import java.awt.Rectangle
import java.awt.Shape
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// This must have the same value as WindowManager.FLAG_DIM_BEHIND
@VisibleForTesting
const val WINDOW_MANAGER_FLAG_DIM_BEHIND = 0x2

/**
 * A view node represents a view in the view hierarchy as seen on the device.
 *
 * @param drawId the View.getUniqueDrawingId which is also the id found in the skia image
 * @param qualifiedName the qualified class name of the view
 * @param layout reference to the layout xml containing this view
 * @param x the left edge of the view from the device left edge, ignoring any post-layout transformations
 * @param y the top edge of the view from the device top edge, ignoring any post-layout transformations
 * @param width the width of this view, ignoring any post-layout transformations
 * @param height the height of this view, ignoring any post-layout transformations
 * @param bounds the actual bounds of this view as shown on the screen, including any post-layout transformations.
 * @param viewId the id set by the developer in the View.id attribute
 * @param textValue the text value if present
 * @param layoutFlags flags from WindowManager.LayoutParams
 */
open class ViewNode(
  var drawId: Long,
  var qualifiedName: String,
  var layout: ResourceReference?,
  var x: Int,
  var y: Int,
  var width: Int,
  var height: Int,
  bounds: Shape?,
  var viewId: ResourceReference?,
  var textValue: String,
  var layoutFlags: Int
) {
  /** constructor for synthetic nodes */
  constructor(qualifiedName: String): this(-1, qualifiedName, null, 0, 0, 0, 0, null, null, "", 0)

  /** The bounds used by android for layout. Always a rectangle. */
  val layoutBounds: Rectangle
    get() = Rectangle(x, y, width, height)

  @Suppress("LeakingThis")
  val treeNode = TreeViewNode(this)

  /** Returns true if this [ViewNode] is found in a layout in the framework or in a system layout from appcompat */
  open val isSystemNode: Boolean
    get() =
      layout == null ||
      layout?.namespace == ResourceNamespace.ANDROID ||
      layout?.name?.startsWith("abc_") == true

  /** Returns true if this [ViewNode] has merged semantics */
  open val hasMergedSemantics: Boolean
    get() = false

  /** Returns true if this [ViewNode] has unmerged semantics */
  open val hasUnmergedSemantics: Boolean
    get() = false

  /**
   * Return the closest unfiltered node
   *
   * This will either be:
   * - the node itself
   * - the closest ancestor that is not filtered out of the component tree
   * - null
   */
  fun findClosestUnfilteredNode(treeSettings: TreeSettings): ViewNode? =
    if (treeSettings.hideSystemNodes) parentSequence.firstOrNull { !it.isSystemNode } else this

  /** Returns true if the node appears in the component tree. False if it currently filtered out */
  fun isInComponentTree(treeSettings: TreeSettings): Boolean =
    treeSettings.isInComponentTree(this)

  /** Returns true if the node represents a call from a parent node with a single call and it itself is making a single call */
  open fun isSingleCall(treeSettings: TreeSettings): Boolean = false

  private var _transformedBounds = bounds

  val transformedBounds: Shape
    get() = _transformedBounds ?: layoutBounds

  fun setTransformedBounds(bounds: Shape?) {
    _transformedBounds = bounds
  }

  private var tagPointer: SmartPsiElementPointer<XmlTag>? = null

  val children = mutableListOf<ViewNode>()
  var parent: ViewNode? = null

  val parentSequence: Sequence<ViewNode>
    get() = generateSequence(this) { it.parent }

  // Views and images that will be drawn.
  // The order here and in children can be different at least due to how compose->view transitions are grafted in.
  private val drawChildren = mutableListOf<DrawViewNode>()

  var tag: XmlTag?
    get() = tagPointer?.element
    set(value) {
      tagPointer = value?.let { SmartPointerManager.getInstance(value.project).createSmartPsiElementPointer(value) }
    }

  val unqualifiedName: String
    get() = qualifiedName.substringAfterLast('.')

  val isDimBehind: Boolean
    get() = (layoutFlags and WINDOW_MANAGER_FLAG_DIM_BEHIND) > 0

  fun flatten(): Sequence<ViewNode> {
    return children.asSequence().flatMap { it.flatten() }.plus(this)
  }

  fun preOrderFlatten(): Sequence<ViewNode> {
    return sequenceOf(this).plus(children.asSequence().flatMap { it.flatten() })
  }

  companion object {
    private val lock = ReentrantReadWriteLock()

    fun <T> readDrawChildren(fn: (ViewNode.() -> List<DrawViewNode>) -> T): T =
      lock.read {
        fn(ViewNode::drawChildren)
      }

    fun writeDrawChildren(fn: (ViewNode.() -> MutableList<DrawViewNode>) -> Unit) =
      lock.write {
        fn(ViewNode::drawChildren)
      }
  }
}