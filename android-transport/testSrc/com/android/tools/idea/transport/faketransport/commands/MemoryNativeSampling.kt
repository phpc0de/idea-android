/*
 * Copyright (C) 2020 The Android Open Source Project
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
import com.android.tools.profiler.proto.Memory

/**
 * Test helper to handle native memory commands. This helper assumes both start and stop are success and return
 * events similar to perfd.
 */
class MemoryNativeSampling(timer: FakeTimer) : CommandHandler(timer) {
  private var startCommandTimestamp = 0L
  override fun handleCommand(command: Command, events: MutableList<Common.Event>) {
    if (command.hasStartNativeSample()) {
      startCommandTimestamp = timer.currentTimeNs
      events.add(Common.Event.newBuilder().apply {
        pid = command.pid
        commandId = command.commandId
        kind = Common.Event.Kind.MEMORY_NATIVE_SAMPLE_STATUS
        timestamp = timer.currentTimeNs
        groupId = startCommandTimestamp
        memoryNativeTrackingStatus = Memory.MemoryNativeTrackingData.newBuilder().apply {
          startTime = timer.currentTimeNs
          status = Memory.MemoryNativeTrackingData.Status.SUCCESS
        }.build()
      }.build())
      events.add(Common.Event.newBuilder().apply {
        pid = command.pid
        commandId = command.commandId
        kind = Common.Event.Kind.MEMORY_NATIVE_SAMPLE_CAPTURE
        timestamp = timer.currentTimeNs
        groupId = startCommandTimestamp
        memoryNativeSample = Memory.MemoryNativeSampleData.newBuilder().apply {
          startTime = timer.currentTimeNs
          endTime = Long.MAX_VALUE
        }.build()
      }.build())

    }
    else {
      events.add(Common.Event.newBuilder().apply {
        pid = command.pid
        commandId = command.commandId
        kind = Common.Event.Kind.MEMORY_NATIVE_SAMPLE_STATUS
        timestamp = timer.currentTimeNs
        groupId = startCommandTimestamp
        memoryNativeTrackingStatus = Memory.MemoryNativeTrackingData.newBuilder().apply {
          startTime = timer.currentTimeNs

          status = Memory.MemoryNativeTrackingData.Status.NOT_RECORDING
        }.build()
      }.build())
      events.add(Common.Event.newBuilder().apply {
        pid = command.pid
        commandId = command.commandId
        kind = Common.Event.Kind.MEMORY_NATIVE_SAMPLE_CAPTURE
        timestamp = timer.currentTimeNs
        groupId = startCommandTimestamp
        memoryNativeSample = Memory.MemoryNativeSampleData.newBuilder().apply {
          startTime = startCommandTimestamp
          endTime = timer.currentTimeNs
        }.build()
      }.build())
    }

  }
}
