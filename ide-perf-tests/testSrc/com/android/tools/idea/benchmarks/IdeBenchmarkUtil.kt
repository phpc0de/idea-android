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
package com.android.tools.idea.benchmarks

import com.android.tools.perflogger.Metric
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.profile.codeInspection.InspectionProfileManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.runInInitMode
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

const val EDITOR_PERFGATE_PROJECT_NAME = "Android Studio Editor"

/**
 * Makes sure that [ApplicationInfoImpl.isInStressTest] returns true, which
 * in turns disables some expensive debug assertions in the platform.
 * This gets us closer to a production setup.
 */
fun disableExpensivePlatformAssertions(fixture: CodeInsightTestFixture) {
  ApplicationInfoImpl.setInStressTest(true)
  Disposer.register(fixture.testRootDisposable,
                    Disposable { ApplicationInfoImpl.setInStressTest(false) })
}

/** Enables all inspections which are on by default in production. */
fun enableAllDefaultInspections(fixture: CodeInsightTestFixture) {
  val defaultProfile = InspectionProfileManager.getInstance().currentProfile
  val tools = runInInitMode { defaultProfile.getAllEnabledInspectionTools(fixture.project) }
  val inspectionEntries = tools.map { it.tool.tool }
  fixture.enableInspections(*inspectionEntries.toTypedArray())
}

/**
 * Blocks until all pending async VFS refreshes have completed.
 * Must *not* be called from the EDT.
 *
 * This helps ensure that a VFS refresh does not happen *during* a benchmark, which
 * can trigger assertion failures in the platform and would anyway add noise to the
 * timing samples.
 */
fun waitForAsyncVfsRefreshes() {
  assert(!ApplicationManager.getApplication().isDispatchThread) {
    "Cannot block on an async VFS refresh from the EDT (otherwise deadlock is imminent)"
  }
  // Here we assume that the RefreshQueue uses FIFO ordering.
  val semaphore = Semaphore(0)
  RefreshQueue.getInstance().refresh(true, false, Runnable(semaphore::release))
  semaphore.acquire()
}

/** Runs [action] several times to warm up, and then several times again. */
fun repeatWithWarmups(
  warmupIterations: Int,
  mainIterations: Int,
  action: (isWarmup: Boolean) -> Unit
) {
  repeat(warmupIterations) {
    val time = measureElapsedMillis { action(true) }
    println("Warmup phase: $time ms")
  }
  repeat(mainIterations) {
    val time = measureElapsedMillis { action(false) }
    println("Main phase: $time ms")
  }
}

/**
 * Runs [action] several times to warm up, and then several times again to measure elapsed time.
 * Returns an array containing a [Metric.MetricSample] for each iteration.
 *
 * Note: if you need more control over time measurement, just use [repeatWithWarmups] directly.
 */
fun measureTimeMs(
  warmupIterations: Int,
  mainIterations: Int,
  setUp: () -> Unit = {},
  action: () -> Unit,
  tearDown: () -> Unit = {}
): List<Metric.MetricSample> {
  val samplesMs = ArrayList<Metric.MetricSample>(mainIterations)
  repeatWithWarmups(
    warmupIterations = warmupIterations,
    mainIterations = mainIterations,
    action = { isWarmup ->
      setUp()
      val timeMs = measureElapsedMillis { action() }
      if (!isWarmup) {
        samplesMs.add(Metric.MetricSample(System.currentTimeMillis(), timeMs))
      }
      tearDown()
    }
  )
  return samplesMs
}

fun <T> runBenchmark(
  recordResults: () -> T,
  runBetweenIterations: () -> Unit,
  commitResults: (List<T>) -> Unit,
  warmupIterations: Int = 100,
  mainIterations: Int = 100
) {
  val samples = mutableListOf<T>()
  runInEdtAndWait {
    // Warmup
    repeat(warmupIterations) {
      recordResults()
      runBetweenIterations()
    }

    // Measure
    repeat(mainIterations) {
      samples.add(recordResults())
      runBetweenIterations()
    }
  }
  commitResults(samples)
}

/**
 * Runs a benchmark intended to measure an operation on a specific set of elements.
 *
 * @param collectElements decides which elements are intended to be used in the benchmark
 * @param warmupAction performs the supplied warmup action on every element
 * @param benchmarkAction performs the benchmark action and returns a benchmark sample for every supplied element
 * @param commitResults collects all samples to be submitted to perfgate or logged elsewhere.
 */
fun <T, S> runBenchmark(
  collectElements: () -> List<T>,
  warmupAction: (T) -> Unit,
  benchmarkAction: (T) -> List<S>,
  commitResults: (List<S>) -> Unit
) {
  val samples = mutableListOf<S>()
  runInEdtAndWait {
    // Warmup
    val elements = collectElements()
    for (element in elements) {
      warmupAction(element)
    }

    // Measure run
    for (element in elements) {
      samples.addAll(benchmarkAction(element))
    }
  }
  commitResults(samples)
}

/**
 * Like [measureTimeMillis], but uses System.nanoTime() under the hood.
 *
 * Justification: System.currentTimeMillis() is not guaranteed to be monotonic, so
 * measureTimeMillis() is not the most robust way to measure elapsed time.
 */
inline fun measureElapsedMillis(action: () -> Unit): Long = TimeUnit.NANOSECONDS.toMillis(measureNanoTime(action))
