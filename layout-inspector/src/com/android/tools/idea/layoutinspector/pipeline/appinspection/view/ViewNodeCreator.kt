/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.layoutinspector.pipeline.appinspection.view

import com.android.tools.idea.layoutinspector.common.StringTable
import com.android.tools.idea.layoutinspector.model.ComposeViewNode
import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.pipeline.InspectorClient.Capability
import com.android.tools.idea.layoutinspector.pipeline.appinspection.compose.ComposeViewNodeCreator
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol
import layoutinspector.view.inspection.LayoutInspectorViewProtocol

private const val ANDROID_VIEWS_HANDLER = "androidx.compose.ui.platform.AndroidViewsHandler"

/**
 * Helper class which handles the logic of converting a [LayoutInspectorViewProtocol.LayoutEvent] into
 * its corresponding [ViewNode]s.
 *
 * @param composeEvent If passed in, and the current view being processed is an AndroidComposeView, this will
 *   be used to generate [ComposeViewNode] children.
 */
class ViewNodeCreator(
  layoutEvent: LayoutInspectorViewProtocol.LayoutEvent,
  composeEvent: LayoutInspectorComposeProtocol.GetComposablesResponse?
) {
  val strings: StringTable = StringTableImpl(layoutEvent.stringsList)
  private val rootView = layoutEvent.rootView
  private val composeNodeCreator = composeEvent?.let { ComposeViewNodeCreator(it) }

  /**
   * The collected capabilities based on the loaded data.
   */
  val dynamicCapabilities: Set<Capability>
    get() = composeNodeCreator?.dynamicCapabilities ?: emptySet()

  fun createRootViewNode(shouldInterrupt: () -> Boolean): ViewNode? {
    return try {
      rootView.convert(shouldInterrupt)
    }
    catch (_: InterruptedException) {
      null
    }
  }

  private fun LayoutInspectorViewProtocol.ViewNode.convert(shouldInterrupt: () -> Boolean): ViewNode {
    if (shouldInterrupt()) {
      throw InterruptedException()
    }

    val view = this

    val qualifiedName = "${strings[view.packageName]}.${strings[view.className]}"
    val resource = view.resource.convert().createReference(strings)
    val layoutResource = view.layoutResource.convert().createReference(strings)
    val textValue = strings[view.textValue]
    val rect = view.bounds.layout
    val renderBounds = view.bounds.render.takeIf { it != LayoutInspectorViewProtocol.Quad.getDefaultInstance() }

    val node = ViewNode(view.id, qualifiedName, layoutResource, rect.x, rect.y, rect.w, rect.h, renderBounds?.toShape(), resource,
                        textValue, view.layoutFlags)

    val children = view.childrenList.map { it.convert(shouldInterrupt) }.toMutableList()
    composeNodeCreator?.createForViewId(view.id, shouldInterrupt)?.forEach { child -> children.add(child) }
    val viewsToSkip = composeNodeCreator?.viewsToSkip?.get(view.id) ?: emptyList()
    children.forEach { child ->
      if (!viewsToSkip.contains(child.drawId)) {
        node.children.add(child)
        child.parent = node
      }
    }

    // Move nodes under ANDROID_VIEWS_HANDLER to their corresponding ComposeNode if the handler is present:
    val handler = node.children.singleOrNull { it.qualifiedName == ANDROID_VIEWS_HANDLER }
    if (handler != null) {
      val viewsToMove = composeNodeCreator?.androidViews?.keys ?: emptyList()
      handler.children.filter { it.drawId in viewsToMove }.forEach {
        composeNodeCreator?.androidViews?.get(it.drawId)?.let { composeParent ->
          composeParent.children.add(it)
          it.parent = composeParent
        }
        handler.children.remove(it)
      }
      // Also remove the handler if all children have been moved
      if (handler.children.isEmpty()) {
        node.children.remove(handler)
        handler.parent = null
      }
    }
    return node
  }
}
