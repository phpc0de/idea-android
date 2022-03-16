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
package com.android.tools.profilers.network;

import com.android.tools.adtui.model.DataSeries;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.SeriesData;
import com.android.tools.profiler.proto.Common;
import com.android.tools.profiler.proto.NetworkProfiler.ConnectionData;
import com.android.tools.profiler.proto.NetworkProfiler.NetworkDataRequest;
import com.android.tools.profiler.proto.NetworkProfiler.NetworkDataResponse;
import com.android.tools.profiler.proto.NetworkProfiler.NetworkProfilerData;
import com.android.tools.profiler.proto.NetworkServiceGrpc;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class is a data series representing the number of connections open at any given time.
 *
 * It is responsible for making an RPC call to perfd/datastore and converting the resulting proto into UI data.
 */
public class NetworkOpenConnectionsDataSeries implements DataSeries<Long> {
  @NotNull private NetworkServiceGrpc.NetworkServiceBlockingStub myClient;
  private final Common.Session mySession;

  public NetworkOpenConnectionsDataSeries(@NotNull NetworkServiceGrpc.NetworkServiceBlockingStub client, @NotNull Common.Session session) {
    myClient = client;
    mySession = session;
  }

  @Override
  public List<SeriesData<Long>> getDataForRange(@NotNull Range timeCurrentRangeUs) {
    List<SeriesData<Long>> seriesData = new ArrayList<>();

    // TODO: Change the Network API to allow specifying padding in the request as number of samples.
    long bufferNs = TimeUnit.SECONDS.toNanos(1);
    NetworkDataRequest.Builder dataRequestBuilder = NetworkDataRequest.newBuilder()
      .setSession(mySession)
      .setType(NetworkDataRequest.Type.CONNECTIONS)
      .setStartTimestamp(TimeUnit.MICROSECONDS.toNanos((long)timeCurrentRangeUs.getMin()) - bufferNs)
      .setEndTimestamp(TimeUnit.MICROSECONDS.toNanos((long)timeCurrentRangeUs.getMax()) + bufferNs);
    NetworkDataResponse response = myClient.getData(dataRequestBuilder.build());
    for (NetworkProfilerData data : response.getDataList()) {
      long xTimestamp = TimeUnit.NANOSECONDS.toMicros(data.getEndTimestamp());
      ConnectionData connectionData = data.getConnectionData();
      seriesData.add(new SeriesData<>(xTimestamp, (long)connectionData.getConnectionNumber()));
    }
    return seriesData;
  }
}
