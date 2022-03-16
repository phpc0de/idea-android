/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.profilers;

import static com.android.tools.idea.transport.faketransport.FakeTransportService.VERSION;
import static com.google.common.truth.Truth.assertThat;

import com.android.sdklib.AndroidVersion;
import com.android.tools.adtui.model.FakeTimer;
import com.android.tools.idea.transport.faketransport.FakeGrpcServer;
import com.android.tools.idea.transport.faketransport.FakeTransportService;
import com.android.tools.profiler.proto.Common;
import com.android.tools.profiler.proto.Transport.VersionRequest;
import com.android.tools.profiler.proto.Transport.VersionResponse;
import com.android.tools.profilers.cpu.CpuProfilerStage;
import com.android.tools.profilers.cpu.CpuProfilerTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public final class StudioProfilersCommonTest {
  private final FakeTimer myTimer = new FakeTimer();
  private final FakeTransportService myTransportService = new FakeTransportService(myTimer, false);
  @Rule public FakeGrpcServer myGrpcServer =
    FakeGrpcServer.createFakeGrpcServer("StudioProfilerCommonTestChannel", myTransportService, new FakeProfilerService(myTimer));
  private ProfilerClient myProfilerClient;

  @Before
  public void setUp() {
    myProfilerClient = new ProfilerClient(myGrpcServer.getChannel());
  }

  @Test
  public void testVersion() {
    VersionResponse response =
      myProfilerClient.getTransportClient().getVersion(VersionRequest.getDefaultInstance());
    assertThat(response.getVersion()).isEqualTo(VERSION);
  }

  @Test
  public void testClearedOnMonitorStage() {
    StudioProfilers profilers = getProfilersWithDeviceAndProcess();
    assertThat(profilers.getTimeline().getSelectionRange().isEmpty()).isTrue();

    profilers.setStage(new CpuProfilerStage(profilers));
    profilers.getTimeline().getSelectionRange().set(10, 10);
    profilers.setMonitoringStage();

    assertThat(profilers.getTimeline().getSelectionRange().isEmpty()).isTrue();
  }

  @Test
  public void testProfilerModeChange() throws Exception {
    StudioProfilers profilers = getProfilersWithDeviceAndProcess();
    assertThat(profilers.getMode()).isEqualTo(ProfilerMode.NORMAL);
    CpuProfilerStage stage = new CpuProfilerStage(profilers);
    profilers.setStage(stage);
    assertThat(profilers.getMode()).isEqualTo(ProfilerMode.NORMAL);
    stage.setAndSelectCapture(CpuProfilerTestUtils.getValidCapture());
    assertThat(profilers.getMode()).isEqualTo(ProfilerMode.EXPANDED);
    profilers.setMonitoringStage();
    assertThat(profilers.getMode()).isEqualTo(ProfilerMode.NORMAL);
  }

  private StudioProfilers getProfilersWithDeviceAndProcess() {
    FakeTimer timer = new FakeTimer();
    StudioProfilers profilers = new StudioProfilers(myProfilerClient, new FakeIdeProfilerServices(), timer);
    timer.tick(FakeTimer.ONE_SECOND_IN_NS);

    Common.Device device = Common.Device.newBuilder()
      .setDeviceId("FakeDevice".hashCode())
      .setFeatureLevel(AndroidVersion.VersionCodes.BASE)
      .setSerial("FakeDevice")
      .setState(Common.Device.State.ONLINE)
      .build();
    myTransportService.addDevice(device);
    timer.tick(FakeTimer.ONE_SECOND_IN_NS); // One second must be enough for new devices to be picked up
    profilers.setProcess(device, null);
    assertThat(profilers.getDevice()).isEqualTo(device);
    assertThat(profilers.getProcess()).isNull();

    Common.Process process = Common.Process.newBuilder()
      .setDeviceId(device.getDeviceId())
      .setPid(20)
      .setName("FakeProcess")
      .setState(Common.Process.State.ALIVE)
      .build();
    myTransportService.addProcess(device, process);
    timer.tick(FakeTimer.ONE_SECOND_IN_NS); // One second must be enough for new devices to be picked up
    profilers.setProcess(device, process);

    assertThat(profilers.getProcess()).isEqualTo(process);
    return profilers;
  }
}
