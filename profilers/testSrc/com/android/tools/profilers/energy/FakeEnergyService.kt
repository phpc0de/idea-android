// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.android.tools.profilers.energy

import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.EnergyProfiler
import com.android.tools.profiler.proto.EnergyProfiler.EnergyEventGroupRequest
import com.android.tools.profiler.proto.EnergyProfiler.EnergyEventsResponse
import com.android.tools.profiler.proto.EnergyProfiler.EnergyRequest
import com.android.tools.profiler.proto.EnergyProfiler.EnergySample
import com.android.tools.profiler.proto.EnergyProfiler.EnergySamplesResponse
import com.android.tools.profiler.proto.EnergyProfiler.EnergyStartRequest
import com.android.tools.profiler.proto.EnergyProfiler.EnergyStartResponse
import com.android.tools.profiler.proto.EnergyProfiler.EnergyStopRequest
import com.android.tools.profiler.proto.EnergyProfiler.EnergyStopResponse
import com.android.tools.profiler.proto.EnergyServiceGrpc
import io.grpc.stub.StreamObserver
import java.util.stream.Collectors

class FakeEnergyService(val dataList: List<EnergySample> = ArrayList(), val eventList: List<Common.Event> = ArrayList())
  : EnergyServiceGrpc.EnergyServiceImplBase() {

  lateinit var session: Common.Session

  override fun startMonitoringApp(request: EnergyStartRequest, responseObserver: StreamObserver<EnergyStartResponse>) {
    session = request.session
    responseObserver.onNext(EnergyStartResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun stopMonitoringApp(request: EnergyStopRequest, responseObserver: StreamObserver<EnergyStopResponse>) {
    session = request.session
    responseObserver.onNext(EnergyStopResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }

  override fun getSamples(request: EnergyRequest, responseObserver: StreamObserver<EnergySamplesResponse>) {
    val listStream = dataList.stream().filter { d -> d.timestamp >= request.startTimestamp && d.timestamp < request.endTimestamp }
    val resultList = listStream.collect(Collectors.toList())
    val response = EnergySamplesResponse.newBuilder().addAllSamples(resultList).build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun getEvents(request: EnergyRequest, responseObserver: StreamObserver<EnergyProfiler.EnergyEventsResponse>) {
    val listStream = eventList.stream().filter { d -> d.timestamp >= request.startTimestamp && d.timestamp < request.endTimestamp }
    val response = EnergyProfiler.EnergyEventsResponse.newBuilder().addAllEvents(listStream.collect(Collectors.toList())).build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun getEventGroup(request: EnergyEventGroupRequest, responseObserver: StreamObserver<EnergyEventsResponse>) {
    val eventsWithRequestedId = eventList.stream().filter { e -> e.groupId == request.eventId }.collect(Collectors.toList())
    val response = EnergyProfiler.EnergyEventsResponse.newBuilder().addAllEvents(eventsWithRequestedId).build()
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }
}
