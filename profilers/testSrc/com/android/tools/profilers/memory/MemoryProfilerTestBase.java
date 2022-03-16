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
package com.android.tools.profilers.memory;

import static com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_DEVICE_NAME;
import static com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_PROCESS_NAME;

import com.android.tools.adtui.model.FakeTimer;
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel;
import com.android.tools.profilers.FakeIdeProfilerServices;
import com.android.tools.profilers.ProfilerClient;
import com.android.tools.profilers.StudioProfilers;
import com.android.tools.profilers.memory.adapters.CaptureObject;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;

import java.util.concurrent.Executor;

public abstract class MemoryProfilerTestBase {
  protected StudioProfilers myProfilers;
  protected MainMemoryProfilerStage myStage;
  protected FakeCaptureObjectLoader myMockLoader;
  protected MemoryAspectObserver myAspectObserver;
  protected FakeTimer myTimer = new FakeTimer();
  protected FakeIdeProfilerServices myIdeProfilerServices;

  @Before
  public void setupBase() {
    myIdeProfilerServices = new FakeIdeProfilerServices();
    myProfilers = new StudioProfilers(new ProfilerClient(getGrpcChannel().getChannel()), myIdeProfilerServices, myTimer);
    myProfilers.setPreferredProcess(FAKE_DEVICE_NAME, FAKE_PROCESS_NAME, null);
    onProfilersCreated(myProfilers);

    myMockLoader = new FakeCaptureObjectLoader();
    myStage = new MainMemoryProfilerStage(myProfilers, myMockLoader);
    myAspectObserver = new MemoryAspectObserver(myStage.getAspect(), myStage.getCaptureSelection().getAspect());

    // Advance the clock to make sure StudioProfilers has a chance to select device + process.
    myTimer.tick(FakeTimer.ONE_SECOND_IN_NS);
    myProfilers.setStage(myStage);
  }

  /**
   * Child classes are responsible for providing their own fake grpc channel
   */
  protected abstract FakeGrpcChannel getGrpcChannel();

  protected void onProfilersCreated(StudioProfilers profilers) {
  }
}
