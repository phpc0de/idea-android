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
package com.android.tools.idea.layoutinspector.metrics.statistics

import com.google.wireless.android.sdk.stats.DynamicLayoutInspectorRotation

/**
 * Accumulator of rotation statistics for analytics.
 */
class RotationStatistics {
  /**
   * How many clicks on a component in the image did the user perform in 3D mode
   */
  private var imageClicksIn3D = 0

  /**
   * How many clicks on a component in the image did the user perform in 2D mode
   */
  private var imageClicksIn2D = 0

  /**
   * How many clicks on a component in the component tree did the user perform in 3D mode
   */
  private var componentTreeClicksIn3D = 0

  /**
   * How many clicks on a component in the component tree did the user perform in 2D mode
   */
  private var componentTreeClicksIn2D = 0

  private var currentMode3D = false

  /**
   * Start a new session by resetting all counters.
   */
  fun start() {
    currentMode3D = false
    imageClicksIn3D = 0
    imageClicksIn2D = 0
    componentTreeClicksIn3D = 0
    componentTreeClicksIn2D = 0
  }

  /**
   * Save the session data recorded since [start].
   */
  fun save(data: DynamicLayoutInspectorRotation.Builder) {
    data.imageClicksIn2D = imageClicksIn2D
    data.imageClicksIn3D = imageClicksIn3D
    data.componentTreeClicksIn2D = componentTreeClicksIn2D
    data.componentTreeClicksIn3D = componentTreeClicksIn3D
  }

  /**
   * Log that the user switched to a 3D view.
   */
  fun toggledTo3D() {
    currentMode3D = true
  }

  /**
   * Log that the user switched to a 2D view.
   */
  fun toggledTo2D() {
    currentMode3D = false
  }

  /**
   * Log that a component was selected from the image.
   */
  fun selectionMadeFromImage() {
    if (currentMode3D) imageClicksIn3D++ else imageClicksIn2D++
  }

  /**
   * Log that a component was selected from the component tree.
   */
  fun selectionMadeFromComponentTree() {
    if (currentMode3D) componentTreeClicksIn3D++ else componentTreeClicksIn2D++
  }
}
