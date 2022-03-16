package com.android.tools.profilers.memory

import com.android.tools.adtui.model.Range
import com.android.tools.adtui.model.SeriesData
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.memory.BaseStreamingMemoryProfilerStage.LiveAllocationSamplingMode.Companion.getModeFromFrequency
import com.android.tools.profilers.memory.BaseStreamingMemoryProfilerStage.LiveAllocationSamplingMode.FULL
import com.android.tools.profilers.memory.BaseStreamingMemoryProfilerStage.LiveAllocationSamplingMode.NONE
import com.android.tools.profilers.memory.adapters.CaptureObject
import com.google.wireless.android.sdk.stats.AndroidProfilerEvent
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * This class implements a stage tracking allocations, either live or finished
 */
class AllocationStage private constructor(profilers: StudioProfilers, loader: CaptureObjectLoader, initMinUs: Double, initMaxUs: Double)
  : BaseStreamingMemoryProfilerStage(profilers, loader) {

  // The boundaries of the allocation tracking period.
  // - If tracking hasn't started yet, `minTrackingTimeUs` is -∞
  // - If live tracking session hasn't finished yet, `maxTrackingTimeUs` is +∞
  var minTrackingTimeUs = initMinUs
    private set
  var maxTrackingTimeUs = initMaxUs
    private set
  private val hasStartedTracking get() = minTrackingTimeUs > NEGATIVE_INFINITY
  val hasEndedTracking get() = maxTrackingTimeUs < POSITIVE_INFINITY
  private val isStatic get() = hasStartedTracking && hasEndedTracking

  private val allocationDurationData = makeModel(CaptureDataSeries::ofAllocationInfos)
  override val captureSeries get() = listOf(allocationDurationData)

  override fun getParentStage() = MainMemoryProfilerStage(studioProfilers, loader)
  override fun getHomeStageClass() = MainMemoryProfilerStage::class.java
  override fun isInteractingWithTimeline() = false
  override fun getConfirmExitMessage() = if (hasEndedTracking) null else "Going back will end allocation recording. Proceed?"

  override fun selectCaptureFromSelectionRange() {
    // `doSelectCaptureDuration` can run in the background.
    // If there's a new selection while we're still processing a previous one, we ignore the new one.
    if (updateCaptureOnSelection) {
      updateCaptureOnSelection = false
      doSelectCaptureDuration(getIntersectingCaptureDuration(timeline.selectionRange), SwingUtilities::invokeLater)
      updateCaptureOnSelection = true
    }
  }

  override fun onCaptureToSelect(captureToSelect: SeriesData<CaptureDurationData<out CaptureObject>>, loadJoiner: Executor) =
    doSelectCaptureDuration(captureToSelect.value, loadJoiner)

  fun selectAll() = timeline.selectionRange.set(minTrackingTimeUs, min(maxTrackingTimeUs, timeline.dataRange.max))

  fun isAlmostAllSelected(): Boolean {
    fun Double.almostEqual(that: Double) = abs(this - that) <= 0.001
    return timeline.selectionRange.min.almostEqual(minTrackingTimeUs) &&
           timeline.selectionRange.max.almostEqual(min(maxTrackingTimeUs, timeline.dataRange.max))
  }

  override fun enter() {
    super.enter()
    if (isStatic) {
      timeline.viewRange.set(minTrackingTimeUs, maxTrackingTimeUs)
      timeline.selectionRange.set(minTrackingTimeUs, maxTrackingTimeUs)
    } else {
      // Start tracking when allocation ready
      aspect.addDependency(this).onChange(MemoryProfilerAspect.LIVE_ALLOCATION_SAMPLING_MODE) {
        if (hasStartedTracking) aspect.removeDependencies(this) else startTracking()
      }
      requestLiveAllocationSamplingModeUpdate(FULL)
      // Prevent selecting outside of range
      timeline.selectionRange.apply {
        addDependency(this@AllocationStage).onChange(Range.Aspect.RANGE) {
          val rightBound = min(timeline.dataRange.max, maxTrackingTimeUs)
          when {
            !isEmpty && min > rightBound -> selectAll()
            max > rightBound -> max = rightBound
          }
        }
      }
    }
  }

  override fun exit() {
    super.exit()
    stopTracking()
    timeline.selectionRange.removeDependencies(this)
  }

  private fun startTracking() {
    if (liveAllocationSamplingMode != NONE) {
      val now = timeline.dataRange.max
      minTrackingTimeUs =
        AllocationSamplingRateDataSeries(studioProfilers.client, sessionData, true).getDataForRange(timeline.dataRange)
          .last { it.x.toDouble() <= now && getModeFromFrequency(it.value.currentRate.samplingNumInterval) != NONE }
          .x.toDouble()
      timeline.selectionRange.set(minTrackingTimeUs, minTrackingTimeUs)
      timeline.viewRange.set(minTrackingTimeUs, minTrackingTimeUs)
      timeline.dataRange.addDependency(this).onChange(Range.Aspect.RANGE, ::onNewData)
    }
  }

  fun stopTracking() {
    if (!hasEndedTracking) {
      aspect.removeDependencies(this)
      timeline.dataRange.removeDependencies(this)
      maxTrackingTimeUs = timeline.dataRange.max
    }
    requestLiveAllocationSamplingModeUpdate(NONE)
  }

  private fun onNewData() = with(timeline) {
    assert(!hasEndedTracking)
    val dataMax = dataRange.max
    when {
      dataMax >= viewRange.max -> {
        val initialSessionLengthUs = TimeUnit.SECONDS.toMicros(15).toDouble()
        val length = max(2 * (dataMax - minTrackingTimeUs), initialSessionLengthUs)
        viewRange.set(minTrackingTimeUs, minTrackingTimeUs + length)
      }
      // TODO(b/172492874) fix manually changing timeline's view-range to trigger repaint
      Random.nextBoolean() -> viewRange.max++
      else -> viewRange.max--
    }
    if (selectionRange.min === minTrackingTimeUs || selectionRange.isEmpty) {
      selectionRange.set(minTrackingTimeUs, dataMax)
    }
  }

  override fun getStageType() = AndroidProfilerEvent.Stage.MEMORY_JVM_RECORDING_STAGE

  companion object {
    @JvmStatic @JvmOverloads
    fun makeLiveStage(profilers: StudioProfilers, loader: CaptureObjectLoader = CaptureObjectLoader()) =
      AllocationStage(profilers, loader, NEGATIVE_INFINITY, POSITIVE_INFINITY)

    @JvmStatic @JvmOverloads
    fun makeStaticStage(profilers: StudioProfilers, loader: CaptureObjectLoader = CaptureObjectLoader(),
                        minTrackingTimeUs: Double, maxTrackingTimeUs: Double) =
      AllocationStage(profilers, loader, minTrackingTimeUs, maxTrackingTimeUs).also {
        require(minTrackingTimeUs.isFinite())
        require(maxTrackingTimeUs.isFinite())
        require(minTrackingTimeUs <= maxTrackingTimeUs)
      }
  }
}