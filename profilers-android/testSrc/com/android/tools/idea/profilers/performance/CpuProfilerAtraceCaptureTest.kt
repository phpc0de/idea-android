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
package com.android.tools.idea.profilers.performance

import com.android.tools.profilers.cpu.CpuProfilerTestUtils
import org.junit.Test

/**
 * Atrace test for measuring the memory overhead of loading / parsing a atrace capture.
 * Note: This test is in its own class due to weak/soft reference leaks if the test runner runs other performance test these references
 * impact the memory results of this test. Without any way to force the GC this is the only reliable way to get a stable memory record.
 */
class CpuProfilerAtraceCaptureTest : CpuProfilerMemoryLoadTestBase() {
  @Test
  fun measureMemoryOfImportATrace() {
    loadCaptureAndReport(
      "Atrace-10-sec",
      CpuProfilerTestUtils.getTraceFile("performance/atrace_10s_tanks.trace"),
      "com.google.android.tanks", 7366)
  }
}