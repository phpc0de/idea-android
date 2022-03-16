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

import com.android.tools.adtui.AxisComponent
import com.android.tools.adtui.chart.linechart.LineChart
import com.android.tools.adtui.model.Range
import com.android.tools.adtui.model.SeriesData
import com.android.tools.adtui.model.trackgroup.TrackModel
import com.android.tools.profilers.ProfilerTrackRendererType
import com.android.tools.profilers.cpu.systemtrace.RssMemoryTrackModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RssMemoryTrackRendererTest {
  @Test
  fun render() {
    val rssMemoryTrackModel = TrackModel.newBuilder(
      RssMemoryTrackModel(RSS_MEMORY_COUNTERS, Range()), ProfilerTrackRendererType.RSS_MEMORY, "Foo"
    ).build()
    val component = RssMemoryTrackRenderer().render(rssMemoryTrackModel)
    assertThat(component.componentCount).isEqualTo(2)
    assertThat(component.components[0]).isInstanceOf(AxisComponent::class.java)
    assertThat(component.components[1]).isInstanceOf(LineChart::class.java)
  }

  companion object {
    private val RSS_MEMORY_COUNTERS = listOf(
      SeriesData(0L, 1000L),
      SeriesData(1000L, 2000L),
      SeriesData(2000L, 3000L)
    )
  }
}