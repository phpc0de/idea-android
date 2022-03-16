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
package com.android.tools.idea.common.surface.layout

import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JViewport
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * Abstraction over the [DesignSurface] viewport. In scrollable surfaces, this will wrap a [JViewport]. For non scrolable surfaces, this
 * will simply wrap a [Component].
 */
interface DesignSurfaceViewport {
  val viewRect: Rectangle
  val viewportComponent: Component

  /**
   * The contained view in this viewport. For non scrollable surfaces, this might be the same
   * as [viewportComponent].
   */
  val viewComponent: Component

  var viewPosition: Point

  val extentSize: Dimension
  val viewSize: Dimension

  fun addChangeListener(changeListener: ChangeListener)
}

/**
 * A [DesignSurfaceViewport] for a scrollable surface. This is a direct abstraction over [JViewport].
 */
class ScrollableDesignSurfaceViewport(val viewport: JViewport): DesignSurfaceViewport {
  override val viewRect: Rectangle
    get() = viewport.viewRect
  override val viewportComponent: Component
    get() = viewport
  override val viewComponent: Component
    get() = viewport.view
  override var viewPosition: Point
    get() = viewport.viewPosition
    set(value) {
      viewport.viewPosition = value
    }
  override val extentSize: Dimension
    get() = viewport.extentSize
  override val viewSize: Dimension
    get() = viewport.viewSize

  override fun addChangeListener(changeListener: ChangeListener) = viewport.addChangeListener(changeListener)
}

/**
 * A [DesignSurfaceViewport] for non scrollable surfaces where the viewport and the view are the same.
 */
class NonScrollableDesignSurfaceViewport(val view: Component): DesignSurfaceViewport {
  override val viewRect: Rectangle
    get() = view.bounds
  override val viewportComponent: Component
    get() = view
  override val viewComponent: Component
    get() = view
  override var viewPosition: Point
    get() = Point(0, 0)
    set(_) {}
  override val extentSize: Dimension
    get() = view.size
  override val viewSize: Dimension
  get() = view.size
  override fun addChangeListener(changeListener: ChangeListener) {
    view.addComponentListener(object: ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        changeListener.stateChanged(ChangeEvent(e.source))
      }
    })
  }
}