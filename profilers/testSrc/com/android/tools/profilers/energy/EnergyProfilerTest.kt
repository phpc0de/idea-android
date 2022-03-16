/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.profilers.energy

import com.android.tools.adtui.model.FakeTimer
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.ProfilersTestData
import com.android.tools.profilers.ProfilersTestData.SESSION_DATA
import com.android.tools.profilers.StudioProfilers
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EnergyProfilerTest {

  private val myService = FakeEnergyService()
  @get:Rule
  var myGrpcChannel = FakeGrpcChannel("EnergyProfilerTest", myService)

  private lateinit var myProfiler: EnergyProfiler

  @Before
  fun setUp() {
    val services = FakeIdeProfilerServices().apply { enableEnergyProfiler(true) }
    myProfiler = EnergyProfiler(StudioProfilers(ProfilerClient(myGrpcChannel.channel), services, FakeTimer()))
  }

  @Test
  fun newMonitor() {
    val monitor = myProfiler.newMonitor()
    assertThat(monitor).isNotNull()
    assertThat(monitor).isInstanceOf(EnergyMonitor::class.java)
  }

  @Test
  fun startMonitoring() {
    myProfiler.startProfiling(ProfilersTestData.SESSION_DATA)
    assertThat(myService.session).isEqualTo(SESSION_DATA);
  }

  @Test
  fun stopMonitoring() {
    myProfiler.stopProfiling(ProfilersTestData.SESSION_DATA)
    assertThat(myService.session).isEqualTo(SESSION_DATA);
  }
}