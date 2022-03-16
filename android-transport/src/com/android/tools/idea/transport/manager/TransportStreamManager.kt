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
package com.android.tools.idea.transport.manager

import com.android.annotations.concurrency.AnyThread
import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.Transport.GetEventGroupsRequest
import com.android.tools.profiler.proto.Transport.GetEventGroupsResponse
import com.android.tools.profiler.proto.TransportServiceGrpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import java.util.concurrent.atomic.AtomicBoolean

const val DELAY_MILLIS = 100L

/**
 * Represents stream connection activity.
 */
sealed class StreamActivity(val streamChannel: TransportStreamChannel)

/**
 * Represents a stream connect activity.
 */
class StreamConnected(streamChannel: TransportStreamChannel) : StreamActivity(streamChannel)

/**
 * Represents a stream disconnect activity.
 */
class StreamDisconnected(streamChannel: TransportStreamChannel) : StreamActivity(streamChannel)

/**
 * This simply wraps [Common.Event] and represents an event from a stream channel.
 */
class StreamEvent(val event: Common.Event)

private fun TransportServiceGrpc.TransportServiceBlockingStub.eventGroupFlow(
  pollPeriodMs: Long = DELAY_MILLIS,
  requestBuilder: () -> GetEventGroupsRequest
): Flow<GetEventGroupsResponse> = flow {
  while (true) {
    emit(getEventGroups(requestBuilder()))
    delay(pollPeriodMs)
  }
}

/**
 * Listens to and manages STREAM events from the transport pipeline. Users can subscribe to the activity flow to listen in on new streams.
 *
 * Streams are provided in the form of [TransportStreamChannel], which provides users the ability to register their own listeners while
 * automatically associating them with the stream.
 */
@AnyThread
class TransportStreamManager private constructor(
  private val client: TransportServiceGrpc.TransportServiceBlockingStub,
  private val dispatcher: CoroutineDispatcher
) {
  fun streamActivityFlow(): Flow<StreamActivity> {
    val streams = mutableMapOf<Long, TransportStreamChannel>()
    // Get all streams of all types.
    val request = GetEventGroupsRequest.newBuilder()
      .setStreamId(-1) // DataStoreService.DATASTORE_RESERVED_STREAM_ID
      .setKind(Common.Event.Kind.STREAM)
      .build()
    return client
      .eventGroupFlow { request }
      .transform { response ->
        for (group in response.groupsList) {
          if (group.eventsCount <= 0) continue
          val streamId = group.groupId
          // sort list by timestamp in descending order
          // the latest event may signal the stream is alive or dead:
          // 1) alive - if stream is new, add stream and notify listeners. otherwise do nothing
          // 2) dead - if stream is dead and manager knows it from before, then remove it and notify listeners. Otherwise do nothing.
          val latestEvent = group.eventsList.maxByOrNull { it.timestamp } ?: continue
          val isConnected = latestEvent.stream.hasStreamConnected()
          if (isConnected) {
            if (!streams.containsKey(streamId)) {
              val streamChannel = TransportStreamChannel(latestEvent.stream.streamConnected.stream, client, dispatcher)
              emit(StreamConnected(streamChannel))
              streams[streamId] = streamChannel
            }
          }
          else {
            streams.remove(streamId)?.let { channel ->
              channel.cleanUp()
              emit(StreamDisconnected(channel))
            }
          }
        }
      }
      .flowOn(dispatcher)
  }

  companion object {
    private val managers = mutableListOf<TransportStreamManager>()

    fun createManager(
      client: TransportServiceGrpc.TransportServiceBlockingStub,
      dispatcher: CoroutineDispatcher
    ): TransportStreamManager {
      val manager = TransportStreamManager(client, dispatcher)
      managers.add(manager)
      return manager
    }

    fun unregisterManager(manager: TransportStreamManager) {
      managers.remove(manager)
    }
  }
}

/**
 * Represents the transport channel of a stream, which is a device or emulator.
 *
 * Users can subscribe to a flow of events in this stream by providing their own [StreamEventQuery].
 */
@AnyThread
class TransportStreamChannel(
  val stream: Common.Stream,
  val client: TransportServiceGrpc.TransportServiceBlockingStub,
  private val dispatcher: CoroutineDispatcher
) {

  private val isClosed = AtomicBoolean(false)

  /**
   * Creates and returns a flow based on the filtering criteria indicated by [query].
   */
  fun eventFlow(query: StreamEventQuery): Flow<StreamEvent> {
    var lastTimeStamp = query.startTime?.invoke() ?: Long.MIN_VALUE
    return client
      .eventGroupFlow {
        val builder = GetEventGroupsRequest.newBuilder()
          .setStreamId(stream.streamId)
          .setKind(query.eventKind)
          .setFromTimestamp(lastTimeStamp)
          .setToTimestamp(query.endTime())
        query.processId?.invoke()?.let { builder.pid = it }
        query.groupId?.invoke()?.let { builder.groupId = it }
        builder.build()
      }
      .takeWhile { !isClosed.get() }
      .transform { response ->
        if (response != GetEventGroupsResponse.getDefaultInstance()) {
          val filtered = response.groupsList
            .flatMap { group -> group.eventsList }
            .sortedWith(query.sortOrder)
            .filter { event -> event.timestamp >= lastTimeStamp && query.filter(event) }
          filtered.forEach { event -> emit(StreamEvent(event)) }
          val maxTimeEvent = filtered.maxByOrNull { it.timestamp }
          maxTimeEvent?.let { lastTimeStamp = kotlin.math.max(lastTimeStamp, it.timestamp + 1) }
        }
      }
      .flowOn(dispatcher)
  }

  internal fun cleanUp() {
    isClosed.set(true)
  }
}