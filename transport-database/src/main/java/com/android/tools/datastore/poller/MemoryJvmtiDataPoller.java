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
package com.android.tools.datastore.poller;

import com.android.tools.datastore.database.MemoryLiveAllocationTable;
import com.android.tools.profiler.proto.Common;
import com.android.tools.profiler.proto.Memory;
import com.android.tools.profiler.proto.Memory.BatchAllocationContexts;
import com.android.tools.profiler.proto.Memory.BatchAllocationEvents;
import com.android.tools.profiler.proto.MemoryProfiler;
import com.android.tools.profiler.proto.MemoryProfiler.MemoryData;
import com.android.tools.profiler.proto.MemoryProfiler.MemoryRequest;
import com.android.tools.profiler.proto.MemoryServiceGrpc;
import org.jetbrains.annotations.NotNull;

public class MemoryJvmtiDataPoller extends PollRunner {
  private long myDataRequestStartTimestampNs = Long.MIN_VALUE;
  private final MemoryServiceGrpc.MemoryServiceBlockingStub myPollingService;
  private final MemoryLiveAllocationTable myLiveAllocationTable;
  private final Common.Session mySession;

  public MemoryJvmtiDataPoller(@NotNull Common.Session session,
                               @NotNull MemoryLiveAllocationTable liveAllocationTable,
                               @NotNull MemoryServiceGrpc.MemoryServiceBlockingStub pollingService) {
    super(POLLING_DELAY_NS);
    mySession = session;
    myLiveAllocationTable = liveAllocationTable;
    myPollingService = pollingService;
  }

  @Override
  public void poll() {
    MemoryRequest.Builder dataRequestBuilder = MemoryRequest
      .newBuilder().setSession(mySession).setStartTime(myDataRequestStartTimestampNs).setEndTime(Long.MAX_VALUE);
    MemoryData response = myPollingService.getJvmtiData(dataRequestBuilder.build());

    for (BatchAllocationContexts sample : response.getBatchAllocationContextsList()) {
      myLiveAllocationTable.insertAllocationContexts(mySession, sample);
    }
    for (BatchAllocationEvents sample : response.getBatchAllocationEventsList()) {
      myLiveAllocationTable.insertAllocationEvents(mySession, sample);
    }
    for (Memory.BatchJNIGlobalRefEvent batchJniEvent : response.getJniReferenceEventBatchesList()) {
      myLiveAllocationTable.insertJniReferenceData(mySession, batchJniEvent);
    }
    for (MemoryProfiler.AllocationSamplingRateEvent event : response.getAllocSamplingRateEventsList()) {
      myLiveAllocationTable.insertOrReplaceAllocationSamplingRateEvent(mySession, event);
    }
    if (response.getEndTimestamp() > myDataRequestStartTimestampNs) {
      myDataRequestStartTimestampNs = response.getEndTimestamp();
    }
  }
}