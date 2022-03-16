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

import com.android.tools.adtui.model.DataSeries;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.SeriesData;
import com.android.tools.profiler.proto.Common;
import com.android.tools.profiler.proto.MemoryProfiler;
import com.android.tools.profiler.proto.MemoryServiceGrpc;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LegacyGcStatsDataSeries implements DataSeries<GcDurationData> {
  @NotNull private MemoryServiceGrpc.MemoryServiceBlockingStub myClient;
  @NotNull private final Common.Session mySession;

  public LegacyGcStatsDataSeries(@NotNull MemoryServiceGrpc.MemoryServiceBlockingStub client, @NotNull Common.Session session) {
    myClient = client;
    mySession = session;
  }

  @Override
  public List<SeriesData<GcDurationData>> getDataForRange(@NotNull Range timeCurrentRangeUs) {
    // TODO: Change the Memory API to allow specifying padding in the request as number of samples.
    long bufferNs = TimeUnit.SECONDS.toNanos(1);
    MemoryProfiler.MemoryRequest.Builder dataRequestBuilder = MemoryProfiler.MemoryRequest.newBuilder()
      .setSession(mySession)
      .setStartTime(TimeUnit.MICROSECONDS.toNanos((long)timeCurrentRangeUs.getMin()) - bufferNs)
      .setEndTime(TimeUnit.MICROSECONDS.toNanos((long)timeCurrentRangeUs.getMax()) + bufferNs);
    MemoryProfiler.MemoryData response = myClient.getData(dataRequestBuilder.build());

    List<SeriesData<GcDurationData>> seriesData = new ArrayList<>();
    for (MemoryProfiler.MemoryData.GcStatsSample sample : response.getGcStatsSamplesList()) {
      long dataTimestamp = TimeUnit.NANOSECONDS.toMicros(sample.getStartTime());
      long duration = TimeUnit.NANOSECONDS.toMicros(sample.getEndTime() - sample.getStartTime());
      seriesData.add(new SeriesData<>(dataTimestamp, new GcDurationData(duration)));
    }
    return seriesData;
  }
}
