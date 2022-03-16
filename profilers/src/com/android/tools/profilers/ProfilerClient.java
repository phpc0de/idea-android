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

import com.android.tools.idea.transport.TransportClient;
import com.android.tools.profiler.proto.Commands;
import com.android.tools.profiler.proto.CpuServiceGrpc;
import com.android.tools.profiler.proto.EnergyServiceGrpc;
import com.android.tools.profiler.proto.EventServiceGrpc;
import com.android.tools.profiler.proto.MemoryServiceGrpc;
import com.android.tools.profiler.proto.NetworkServiceGrpc;
import com.android.tools.profiler.proto.ProfilerServiceGrpc;
import com.android.tools.profiler.proto.Transport;
import com.android.tools.profiler.proto.TransportServiceGrpc;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

public class ProfilerClient {

  @NotNull private final TransportServiceGrpc.TransportServiceBlockingStub myTransportClient;
  @NotNull private final ProfilerServiceGrpc.ProfilerServiceBlockingStub myProfilerClient;
  @NotNull private final MemoryServiceGrpc.MemoryServiceBlockingStub myMemoryClient;
  @NotNull private final CpuServiceGrpc.CpuServiceBlockingStub myCpuClient;
  @NotNull private final NetworkServiceGrpc.NetworkServiceBlockingStub myNetworkClient;
  @NotNull private final EventServiceGrpc.EventServiceBlockingStub myEventClient;
  @NotNull private final EnergyServiceGrpc.EnergyServiceBlockingStub myEnergyClient;

  public ProfilerClient(@NotNull String name) {
    // Optimization - In-process direct-executor channel which allows us to communicate between the profiler and transport-database without
    // going through the thread pool. This gives us a speed boost per grpc call plus the full caller's stack in transport-database.
    this(InProcessChannelBuilder.forName(name).usePlaintext().directExecutor().build());
  }

  @VisibleForTesting
  public ProfilerClient(@NotNull ManagedChannel channel) {
    myTransportClient = TransportServiceGrpc.newBlockingStub(channel);
    myProfilerClient = ProfilerServiceGrpc.newBlockingStub(channel);
    myMemoryClient = MemoryServiceGrpc.newBlockingStub(channel);
    myCpuClient = CpuServiceGrpc.newBlockingStub(channel);
    myNetworkClient = NetworkServiceGrpc.newBlockingStub(channel);
    myEventClient = EventServiceGrpc.newBlockingStub(channel);
    myEnergyClient = EnergyServiceGrpc.newBlockingStub(channel);
  }

  @NotNull
  public TransportServiceGrpc.TransportServiceBlockingStub getTransportClient() {
    return myTransportClient;
  }

  public CompletableFuture<Transport.ExecuteResponse> executeAsync(Commands.Command command, Executor executor) {
    return TransportClient.executeAsync(myTransportClient, command, executor);
  }

  @NotNull
  public ProfilerServiceGrpc.ProfilerServiceBlockingStub getProfilerClient() {
    return myProfilerClient;
  }

  @NotNull
  public MemoryServiceGrpc.MemoryServiceBlockingStub getMemoryClient() {
    return myMemoryClient;
  }

  @NotNull
  public CpuServiceGrpc.CpuServiceBlockingStub getCpuClient() {
    return myCpuClient;
  }

  @NotNull
  public NetworkServiceGrpc.NetworkServiceBlockingStub getNetworkClient() {
    return myNetworkClient;
  }

  @NotNull
  public EventServiceGrpc.EventServiceBlockingStub getEventClient() {
    return myEventClient;
  }

  @NotNull
  public EnergyServiceGrpc.EnergyServiceBlockingStub getEnergyClient() {
    return myEnergyClient;
  }
}
