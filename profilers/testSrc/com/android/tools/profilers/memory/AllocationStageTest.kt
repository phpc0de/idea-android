package com.android.tools.profilers.memory

import com.android.tools.adtui.model.FakeTimer
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_DEVICE_NAME
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_PROCESS_NAME
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.FakeProfilerService
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.WithFakeTimer
import com.android.tools.profilers.cpu.FakeCpuService
import com.android.tools.profilers.event.FakeEventService
import com.android.tools.profilers.memory.BaseStreamingMemoryProfilerStage.LiveAllocationSamplingMode.*
import com.android.tools.profilers.network.FakeNetworkService
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AllocationStageTest(private val isLive: Boolean): WithFakeTimer {
  override val timer = FakeTimer()
  private val service = FakeMemoryService()
  private val transportService = FakeTransportService(timer)
  @Rule @JvmField
  val grpcChannel = FakeGrpcChannel("LiveAllocationStageTestChannel", service, transportService,
                                    FakeProfilerService(timer), FakeCpuService(), FakeEventService(),
                                    FakeNetworkService.newBuilder().build())
  private lateinit var profilers: StudioProfilers
  private lateinit var stage: AllocationStage
  private lateinit var mockLoader: FakeCaptureObjectLoader
  private lateinit var observer: MemoryAspectObserver
  private lateinit var ideProfilerServices: FakeIdeProfilerServices

  @Before
  fun setup() {
    ideProfilerServices = FakeIdeProfilerServices()
    profilers = StudioProfilers(ProfilerClient(grpcChannel.channel), ideProfilerServices, timer)
    profilers.setPreferredProcess(FAKE_DEVICE_NAME, FAKE_PROCESS_NAME, null)
    ideProfilerServices.enableEventsPipeline(true)
    mockLoader = FakeCaptureObjectLoader()
    stage = if (isLive) AllocationStage.makeLiveStage(profilers, mockLoader)
            else AllocationStage.makeStaticStage(profilers, minTrackingTimeUs = 1.0, maxTrackingTimeUs = 5.0)
    observer = MemoryAspectObserver(stage.aspect, stage.captureSelection.aspect)

    // Advance the clock to make sure StudioProfilers has a chance to select device + process.
    timer.tick(FakeTimer.ONE_SECOND_IN_NS)
    profilers.stage = stage
  }

  @Test
  fun `stage starts tracking when entered then stops when stopped`() {
    assumeTrue(isLive)
    assertThat(stage.hasEndedTracking).isFalse()
    assertThat(stage.confirmExitMessage).isNotNull()

    stage.stopTracking()
    assertThat(stage.hasEndedTracking).isTrue()
    assertThat(stage.confirmExitMessage).isNull()
    tickOneSec()
    assertThat(stage.liveAllocationSamplingMode).isEqualTo(NONE)
  }

  @Test
  fun `selected range expands over time until stopped`() {
    assumeTrue(isLive)
    val (lo1, hi1) = tickOneSecThen(::getSelectedRange)
    val (lo2, hi2) = tickOneSecThen(::getSelectedRange)
    assertThat(lo1).isEqualTo(stage.minTrackingTimeUs)
    assertThat(lo2).isEqualTo(lo1)
    assertThat(hi2).isGreaterThan(hi1)

    stage.stopTracking()
    val (lo3, hi3) = tickOneSecThen(::getSelectedRange)
    assertThat(lo3).isEqualTo(lo1)
    assertThat(hi3).isEqualTo(hi2)
  }

  @Test
  fun `selecting all satisfies that almost all are selected`() {
    tickOneSecThen(stage::selectAll)
    assertThat(stage.isAlmostAllSelected()).isTrue()
  }

  @Test
  fun `exiting stage disables live allocations`() {
    stage.exit()
    tickOneSec()
    assertThat(stage.getLiveAllocationSamplingModeFromData()).isEqualTo(NONE)
  }

  private fun getSelectedRange() = with(stage.timeline.selectionRange) { Pair(min, max) }

  companion object {
    @Parameterized.Parameters @JvmStatic
    fun isLiveAllocationStage() = listOf(false, true)
  }
}