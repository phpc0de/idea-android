/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.adtui.model.formatter.TimeFormatter
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.profiler.proto.Cpu
import com.android.tools.profiler.proto.CpuProfiler
import com.android.tools.profilers.FakeIdeProfilerComponents
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.FakeProfilerService
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.ProfilersTestData
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.StudioProfilersView
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JLabel

class CpuThreadsTooltipViewTest {
  private val timer = FakeTimer()
  private val transportService = FakeTransportService(timer)
  private val cpuService = FakeCpuService()
  private lateinit var cpuStage: CpuProfilerStage
  private lateinit var selectedCapture: CpuCapture
  private var capturedThreadStateTimeUs = Long.MIN_VALUE
  private lateinit var cpuThreadsTooltip: CpuThreadsTooltip
  private lateinit var cpuThreadsTooltipView: FakeCpuThreadsTooltipView
  @get:Rule
  val myGrpcChannel = FakeGrpcChannel("CpuThreadsTooltipViewTest", cpuService,
                                      transportService, FakeProfilerService(timer))

  @Before
  fun setUp() {
    transportService.addFile("1", CpuProfilerUITestUtils.getTraceContents(CpuProfilerUITestUtils.VALID_TRACE_PATH))
    cpuService.addTraceInfo(Cpu.CpuTraceInfo.newBuilder()
                              .setTraceId(1)
                              .setFromTimestamp(TimeUnit.SECONDS.toNanos(2))
                              .setToTimestamp(TimeUnit.SECONDS.toNanos(4))
                              .setConfiguration(Cpu.CpuTraceConfiguration.newBuilder()
                                                  .setUserOptions(Cpu.CpuTraceConfiguration.UserOptions.newBuilder()
                                                                    .setTraceType(Cpu.CpuTraceType.ART)))
                              .build())

    val profilers = StudioProfilers(ProfilerClient(myGrpcChannel.channel), FakeIdeProfilerServices(), timer)
    cpuStage = CpuProfilerStage(profilers)
    timer.tick(TimeUnit.SECONDS.toNanos(1))
    profilers.stage = cpuStage
    cpuStage.setAndSelectCapture(1)
    selectedCapture = cpuStage.capture!!

    val view = StudioProfilersView(profilers, FakeIdeProfilerComponents())
    val stageView: CpuProfilerStageView = view.stageView as CpuProfilerStageView
    cpuThreadsTooltip = CpuThreadsTooltip(cpuStage.timeline)
    cpuThreadsTooltipView = FakeCpuThreadsTooltipView(stageView, cpuThreadsTooltip)
    cpuStage.tooltip = cpuThreadsTooltip

    stageView.stage.timeline.dataRange.set(0.0, selectedCapture.range.max)
    stageView.stage.timeline.viewRange.set(0.0, selectedCapture.range.max)

    capturedThreadStateTimeUs = selectedCapture.range.max.toLong() - TimeUnit.SECONDS.toMicros(1)
    val capturedThread = CpuProfiler.GetThreadsResponse.ThreadActivity.newBuilder()
      .setNewState(Cpu.CpuThreadData.State.SLEEPING)
      .setTimestamp(TimeUnit.MICROSECONDS.toNanos(capturedThreadStateTimeUs))
      .build()
    cpuService.addThreads(selectedCapture.mainThreadId, "newThread", arrayListOf(capturedThread))
  }

  @Test
  fun textUpdateOnThreadChange() {
    var tooltipTime = TimeUnit.SECONDS.toMicros(1)
    cpuStage.timeline.tooltipRange.set(tooltipTime.toDouble(), tooltipTime.toDouble())
    var threadSeries = LegacyCpuThreadStateDataSeries(ProfilerClient(myGrpcChannel.channel).cpuClient, ProfilersTestData.SESSION_DATA, 1, selectedCapture)

    cpuThreadsTooltip.setThread("myThread", threadSeries)
    var labels = TreeWalker(cpuThreadsTooltipView.tooltipPanel).descendants().filterIsInstance<JLabel>()
    assertThat(labels).hasSize(5) // time, name, state, duration, details unavailable
    assertThat(labels[0].text).isEqualTo("00:01.000")
    assertThat(labels[1].text).isEqualTo("Thread: myThread")
    assertThat(labels[2].text).isEqualTo("Running")
    assertThat(labels[3].text).isEqualTo("7 s") // 1 to 8 seconds
    assertThat(labels[4].text).isEqualTo("Details Unavailable")

    tooltipTime = TimeUnit.SECONDS.toMicros(9) // Should be the last state. Duration is unavailable.
    cpuStage.timeline.tooltipRange.set(tooltipTime.toDouble(), tooltipTime.toDouble())
    labels = TreeWalker(cpuThreadsTooltipView.tooltipPanel).descendants().filterIsInstance<JLabel>()
    assertThat(labels).hasSize(4) // time, name, state, details unavailable
    assertThat(labels[0].text).isEqualTo("00:09.000")
    assertThat(labels[1].text).isEqualTo("Thread: myThread")
    assertThat(labels[2].text).isEqualTo("Dead")
    assertThat(labels[3].text).isEqualTo("Details Unavailable")

    threadSeries = LegacyCpuThreadStateDataSeries(ProfilerClient(myGrpcChannel.channel).cpuClient,
                                                  ProfilersTestData.SESSION_DATA,
                                                  selectedCapture.mainThreadId,
                                                  selectedCapture)
    cpuStage.timeline.tooltipRange.set(capturedThreadStateTimeUs.toDouble(), capturedThreadStateTimeUs.toDouble())
    cpuThreadsTooltip.setThread("newThread", threadSeries)
    labels = TreeWalker(cpuThreadsTooltipView.tooltipPanel).descendants().filterIsInstance<JLabel>()
    assertThat(labels).hasSize(4) // time, name, state, duration
    assertThat(labels[0].text).isEqualTo(TimeFormatter.getSemiSimplifiedClockString(capturedThreadStateTimeUs))
    assertThat(labels[1].text).isEqualTo("Thread: newThread")
    assertThat(labels[2].text).isEqualTo("Sleeping")
    assertThat(labels[3].text).isEqualTo("1 s") // 1 second until the capture finishes
  }

  private class FakeCpuThreadsTooltipView(
    parent: CpuProfilerStageView,
    tooltip: CpuThreadsTooltip)
    : CpuThreadsTooltipView(parent.component, tooltip) {
    val tooltipPanel: JComponent = createComponent()
  }
}
