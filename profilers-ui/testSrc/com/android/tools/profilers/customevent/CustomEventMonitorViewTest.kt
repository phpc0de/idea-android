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
package com.android.tools.profilers.customevent

import com.android.tools.adtui.LegendComponent
import com.android.tools.profilers.ProfilersTestData.DEFAULT_AGENT_ATTACHED_RESPONSE

import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.chart.statechart.StateChart
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.profilers.FakeIdeProfilerComponents
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.StudioProfilersView
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test

class CustomEventMonitorViewTest {

  private val timer = FakeTimer()
  private val transportService = FakeTransportService(timer, true)
  private lateinit var monitorView: CustomEventMonitorView

  @Before
  fun setUp() {
    val profilers = StudioProfilers(ProfilerClient("CustomEventMonitorViewTestChannel"), FakeIdeProfilerServices(), timer)
    transportService.setAgentStatus(DEFAULT_AGENT_ATTACHED_RESPONSE)
    timer.tick(TimeUnit.SECONDS.toNanos(1))

    val profilerView = StudioProfilersView(profilers, FakeIdeProfilerComponents())
    monitorView = CustomEventMonitorView(profilerView, CustomEventMonitor(profilers))
  }

  @Test
  fun testMonitorStateChart() {
    // Test that the state chart has been added to the monitor view.
    val treeWalker = TreeWalker(monitorView.component)
    val stateChartList = treeWalker.descendants().filterIsInstance(StateChart::class.java)
    assertThat(stateChartList).isNotNull()
    assertThat(stateChartList.size).isEqualTo(1)
  }

  @Test
  fun testMonitorLegend() {
    // Test that the legend component has been added to the monitor view.
    val treeWalker = TreeWalker(monitorView.component)
    val stateChartList = treeWalker.descendants().filterIsInstance(LegendComponent::class.java)
    assertThat(stateChartList).isNotNull()
    assertThat(stateChartList.size).isEqualTo(1)
  }
}