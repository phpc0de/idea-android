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
package com.android.tools.idea.appinspection.inspectors.network.model

import com.android.tools.adtui.model.AspectModel
import com.android.tools.adtui.model.Range
import com.android.tools.adtui.model.StopwatchTimer
import com.android.tools.adtui.model.StreamingTimeline
import com.android.tools.adtui.model.axis.ResizingAxisComponentModel
import com.android.tools.adtui.model.formatter.TimeAxisFormatter
import com.android.tools.adtui.model.updater.Updater
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Executor


/**
 * Contains the suite of services on which the network inspector relies. Ex: Timeline and updater.
 *
 * It also contains a [backgroundExecutor] that is used by the LineChartModel to perform computation on.
 * Tests can choose to swap it out with a direct executor to make life easier.
 */
class NetworkInspectorServices(
  val navigationProvider: CodeNavigationProvider,
  startTimeStampNs: Long,
  timer: StopwatchTimer,
  val backgroundExecutor: Executor = AppExecutorUtil.getAppExecutorService()
) : AspectModel<NetworkInspectorAspect>() {
  val updater: Updater = Updater(timer)
  val timeline = StreamingTimeline(updater)
  val viewAxis = ResizingAxisComponentModel.Builder(timeline.viewRange, TimeAxisFormatter.DEFAULT)
    .setGlobalRange(timeline.dataRange).build()

  init {
    timeline.selectionRange.addDependency(this).onChange(Range.Aspect.RANGE) {
      if (!timeline.selectionRange.isEmpty) {
        timeline.isStreaming = false
      }
    }
    timeline.reset(startTimeStampNs, startTimeStampNs)
  }
}