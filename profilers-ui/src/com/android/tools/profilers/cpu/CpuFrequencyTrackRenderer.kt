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
package com.android.tools.profilers.cpu

import com.android.tools.adtui.chart.linechart.LineChart
import com.android.tools.adtui.chart.linechart.LineConfig
import com.android.tools.adtui.common.DataVisualizationColors
import com.android.tools.adtui.model.trackgroup.TrackModel
import com.android.tools.adtui.trackgroup.TrackRenderer
import com.android.tools.profilers.ProfilerTrackRendererType
import com.android.tools.profilers.cpu.systemtrace.CpuFrequencyTrackModel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Track renderer for System Trace CPU frequency counters.
 */
class CpuFrequencyTrackRenderer : TrackRenderer<CpuFrequencyTrackModel, ProfilerTrackRendererType> {
  override fun render(trackModel: TrackModel<CpuFrequencyTrackModel, ProfilerTrackRendererType>): JComponent {
    return JPanel(BorderLayout()).apply {
      val lineChartModel = trackModel.dataModel
      val lineChart = LineChart(lineChartModel).apply {
        configure(lineChartModel.cpuFrequencySeries,
                  LineConfig(DataVisualizationColors.getColor(trackModel.title.hashCode())).setFilled(true).setStepped(true))
        setFillEndGap(true)
      }
      add(lineChart)
    }
  }
}