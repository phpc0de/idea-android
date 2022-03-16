/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.tools.profiler.proto.Common.Event.EventGroupIds.NETWORK_RX_VALUE;
import static com.android.tools.profiler.proto.Common.Event.EventGroupIds.NETWORK_TX_VALUE;

import com.android.tools.adtui.model.FakeTimer;
import com.android.tools.adtui.model.Range;
import com.android.tools.adtui.model.SeriesData;
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel;
import com.android.tools.idea.transport.faketransport.FakeTransportService;
import com.android.tools.profiler.proto.Common;
import com.google.common.truth.Truth;
import com.intellij.util.containers.ContainerUtil;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class UnifiedEventDataSeriesTest {
  // Use an arbitrary stream id because we don't care in the data series.
  private static final int STREAM_ID = 1;

  private final FakeTransportService myService = new FakeTransportService(new FakeTimer());
  @Rule public FakeGrpcChannel myGrpcChannel = new FakeGrpcChannel("UnifiedEventDataSeriesTest", myService);

  @Test
  public void testGetDataForXRange() {
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkTxEvent(3, 30).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkTxEvent(1, 10).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkTxEvent(5, 50).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkRxEvent(2, 20).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkRxEvent(4, 40).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkRxEvent(6, 60).build());

    UnifiedEventDataSeries<Long> series1 = new UnifiedEventDataSeries<>(new ProfilerClient(myGrpcChannel.getChannel()).getTransportClient(),
                                                                        STREAM_ID,
                                                                        0,
                                                                        Common.Event.Kind.NETWORK_SPEED,
                                                                        NETWORK_TX_VALUE,
                                                                        UnifiedEventDataSeries.fromFieldToDataExtractor(
                                                                          event -> event.getNetworkSpeed().getThroughput()));
    List<SeriesData<Long>> data1 = series1.getDataForRange(new Range(Integer.MIN_VALUE, Integer.MAX_VALUE));
    Truth.assertThat(ContainerUtil.map(data1, data -> data.x)).containsExactly(1L, 3L, 5L);
    Truth.assertThat(ContainerUtil.map(data1, data -> data.value)).containsExactly(10L, 30L, 50L);

    UnifiedEventDataSeries<Long> series2 = new UnifiedEventDataSeries<>(new ProfilerClient(myGrpcChannel.getChannel()).getTransportClient(),
                                                                        STREAM_ID,
                                                                        0,
                                                                        Common.Event.Kind.NETWORK_SPEED,
                                                                        NETWORK_RX_VALUE,
                                                                        UnifiedEventDataSeries.fromFieldToDataExtractor(
                                                                          event -> event.getNetworkSpeed().getThroughput()));
    List<SeriesData<Long>> data2 = series2.getDataForRange(new Range(Integer.MIN_VALUE, Integer.MAX_VALUE));
    Truth.assertThat(ContainerUtil.map(data2, data -> data.x)).containsExactly(2L, 4L, 6L);
    Truth.assertThat(ContainerUtil.map(data2, data -> data.value)).containsExactly(20L, 40L, 60L);
  }

  @Test(expected = AssertionError.class)
  public void testAssertOnMultipleGroupData() {
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkTxEvent(1, 10).build());
    myService.addEventToStream(STREAM_ID, ProfilersTestData.generateNetworkRxEvent(2, 20).build());

    // Querying a multiple-group data kind without a group id triggers an assert.
    UnifiedEventDataSeries<Long> series1 = new UnifiedEventDataSeries<>(new ProfilerClient(myGrpcChannel.getChannel()).getTransportClient(),
                                                                        STREAM_ID,
                                                                        0,
                                                                        Common.Event.Kind.NETWORK_SPEED,
                                                                        UnifiedEventDataSeries.DEFAULT_GROUP_ID,
                                                                        UnifiedEventDataSeries.fromFieldToDataExtractor(
                                                                          event -> event.getNetworkSpeed().getThroughput()));
    series1.getDataForRange(new Range(Integer.MIN_VALUE, Integer.MAX_VALUE));
  }
}