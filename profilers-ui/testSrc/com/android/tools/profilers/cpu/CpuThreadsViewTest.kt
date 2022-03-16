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

import com.android.testutils.TestUtils.resolveWorkspacePath
import com.android.tools.adtui.DragAndDropList
import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.adtui.ui.HideablePanel
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_DEVICE_NAME
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_PROCESS_NAME
import com.android.tools.profiler.proto.Cpu
import com.android.tools.profiler.proto.CpuProfiler
import com.android.tools.profilers.FakeFeatureTracker
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.FakeProfilerService
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.ProfilerColors
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.cpu.capturedetails.CaptureModel
import com.android.tools.profilers.event.FakeEventService
import com.android.tools.profilers.memory.FakeMemoryService
import com.android.tools.profilers.network.FakeNetworkService
import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.swing.JLabel
import javax.swing.ListSelectionModel

@RunsInEdt
class CpuThreadsViewTest {
  private val timer = FakeTimer()
  private val transportService = FakeTransportService(timer)
  private val cpuService = FakeCpuService()

  @get:Rule
  var grpcChannel = FakeGrpcChannel("CpuThreadsViewTest", cpuService,
                                    transportService, FakeProfilerService(timer),
                                    FakeMemoryService(), FakeEventService(), FakeNetworkService.newBuilder().build())
  @get:Rule val myEdtRule = EdtRule()

  private lateinit var stage: CpuProfilerStage
  private lateinit var ideServices: FakeIdeProfilerServices

  @Before
  fun setUp() {
    ideServices = FakeIdeProfilerServices()
    val profilers = StudioProfilers(ProfilerClient(grpcChannel.channel), ideServices, timer)
    profilers.setPreferredProcess(FAKE_DEVICE_NAME, FAKE_PROCESS_NAME, null)
    timer.tick(FakeTimer.ONE_SECOND_IN_NS)

    stage = CpuProfilerStage(profilers)
    stage.studioProfilers.stage = stage
    stage.enter()
  }

  @Test
  fun selectedThreadReflectOnTheModel() {
    val threadsView = CpuThreadsView(stage)
    // Make a selection that includes all threads in the model.
    stage.timeline.viewRange.set(-Double.MAX_VALUE, Double.MAX_VALUE)
    val threadsList = getThreadsList(threadsView)
    val tracker = ideServices.featureTracker as FakeFeatureTracker
    assertThat(threadsList.selectedValue).isNull()
    assertThat(stage.selectedThread).isEqualTo(CaptureModel.NO_THREAD)
    assertThat(tracker.isTrackSelectThreadCalled).isFalse()

    threadsList.selectedIndex = 0
    assertThat(threadsList.selectedValue).isNotNull()
    assertThat(stage.selectedThread).isEqualTo(threadsList.selectedValue.threadId)
    assertThat(tracker.isTrackSelectThreadCalled).isTrue()
  }

  @Test
  fun verifyTitleContent() {
    val threadsView = CpuThreadsView(stage)
    val title = TreeWalker(threadsView.component).descendants().filterIsInstance(JLabel::class.java).first().text
    // Text is actual an HTML, so we use contains instead of equals
    assertThat(title).contains("THREADS")
  }

  @Test
  fun testHideablePanelsHaveItemCountsAsTitle() {
    val threadsView = CpuThreadsView(stage)
    stage.studioProfilers.stage = stage
    stage.profilerConfigModel.profilingConfiguration = FakeIdeProfilerServices.ATRACE_CONFIG
    CpuProfilerTestUtils.captureSuccessfully(
      stage,
      cpuService,
      transportService,
      CpuProfilerTestUtils.traceFileToByteString(resolveWorkspacePath(CpuProfilerUITestUtils.ATRACE_TRACE_PATH).toFile()))
    stage.timeline.viewRange.set(stage.capture!!.range)

    // Find our thread list.
    val hideablePanel = TreeWalker(threadsView.component).ancestors().filterIsInstance<HideablePanel>().first()
    val panelTitle = TreeWalker(hideablePanel).descendants().filterIsInstance<JLabel>().first()
    assertThat(panelTitle.text).contains("THREADS (0)")
    // Add a thread
    cpuService.addThreads(1, "Test", mutableListOf(
      CpuProfiler.GetThreadsResponse.ThreadActivity.newBuilder().setTimestamp(0).setNewState(
        Cpu.CpuThreadData.State.SLEEPING).build()))
    // Update the view range triggering an aspect change in CpuThreadsModel.
    stage.timeline.viewRange.set(stage.timeline.dataRange)
    // Tick to trigger
    timer.tick(FakeTimer.ONE_SECOND_IN_NS)
    assertThat(panelTitle.text).contains("THREADS (1)")
  }

  @Test
  fun scrollPaneViewportViewShouldBeThreadsView() {
    val threadsView = CpuThreadsView(stage)
    val descendants = TreeWalker(threadsView.component).descendants().filterIsInstance(CpuListScrollPane::class.java)
    assertThat(descendants).hasSize(1)
    val scrollPane = descendants[0]

    assertThat(scrollPane.viewport.view).isEqualTo(getThreadsList(threadsView))
  }

  @Test
  fun selectionModelShouldBeSingleSelection() {
    val threadsView = CpuThreadsView(stage)
    assertThat(getThreadsList(threadsView).selectionModel.selectionMode).isEqualTo(ListSelectionModel.SINGLE_SELECTION)
  }

  @Test
  fun threadsViewShouldHaveNullBorder() {
    val threadsView = CpuThreadsView(stage)
    assertThat(getThreadsList(threadsView).border).isNull()
  }

  @Test
  fun cellRendererShouldBeThreadCellRenderer() {
    val threadsView = CpuThreadsView(stage)
    assertThat(getThreadsList(threadsView).cellRenderer).isInstanceOf(ThreadCellRenderer::class.java)
  }

  @Test
  fun backgroundShouldBeDefaultStage() {
    val threadsView = CpuThreadsView(stage)
    assertThat(getThreadsList(threadsView).background).isEqualTo(ProfilerColors.DEFAULT_STAGE_BACKGROUND)
  }

  private fun getThreadsList(threadsView: CpuThreadsView) = TreeWalker(threadsView.component)
    .descendants()
    .filterIsInstance<DragAndDropList<CpuThreadsModel.RangedCpuThread>>()
    .first()
}