/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.transport.faketransport.commands

import com.android.tools.adtui.model.FakeTimer
import com.android.tools.profiler.proto.Commands.Command
import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.Cpu

class StartCpuTrace(timer: FakeTimer) : CommandHandler(timer) {
  var startStatus: Cpu.TraceStartStatus = Cpu.TraceStartStatus.getDefaultInstance()

  override fun handleCommand(command: Command, events: MutableList<Common.Event>) {
    val traceId = timer.currentTimeNs
    // Inserts an in-progress trace info object, which the stage will see on the next time update.
    val info = Cpu.CpuTraceInfo.newBuilder()
      .setTraceId(traceId)
      .setFromTimestamp(traceId)
      .setToTimestamp(-1)
      .setConfiguration(command.startCpuTrace.configuration)
      .setStartStatus(startStatus)
      .build()

    events.add(Common.Event.newBuilder().apply {
      groupId = traceId
      pid = command.pid
      kind = Common.Event.Kind.CPU_TRACE_STATUS
      timestamp = timer.currentTimeNs
      commandId = command.commandId
      cpuTraceStatus = Cpu.CpuTraceStatusData.newBuilder().apply {
        traceStartStatus = startStatus
      }.build()
    }.build())

    if (startStatus.status == Cpu.TraceStartStatus.Status.SUCCESS) {
      events.add(Common.Event.newBuilder().apply {
        groupId = traceId
        pid = command.pid
        kind = Common.Event.Kind.CPU_TRACE
        timestamp = timer.currentTimeNs
        cpuTrace = Cpu.CpuTraceData.newBuilder().apply {
          traceStarted = Cpu.CpuTraceData.TraceStarted.newBuilder().apply {
            traceInfo = info
          }.build()
        }.build()
      }.build())
    }
  }
}
