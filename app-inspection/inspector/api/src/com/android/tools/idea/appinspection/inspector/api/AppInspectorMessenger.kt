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
package com.android.tools.idea.appinspection.inspector.api

import com.android.annotations.concurrency.WorkerThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * A class that facilitates two-way communication between ourselves (the host) and the app
 * inspector that lives on the device.
 */
interface AppInspectorMessenger {
  /**
   * Sends a raw command using the provided [rawData]. Returns the raw response.
   *
   * This function can throw [AppInspectionConnectionException] which happens when App Inspection framework encounters an
   * issue with the underlying connection to the process. Clients must be able to handle it gracefully.
   * An example would be the connection ended because the app crashed.
   */
  @WorkerThread
  suspend fun sendRawCommand(rawData: ByteArray): ByteArray

  /**
   * A cold data stream of events sent by the inspector on device.
   *
   * Note: Once the inspector client is disposed by any means, collection won't be possible and will result in
   * CancellationException being thrown immediately.
   */
  val eventFlow: Flow<ByteArray>

  /**
   * The coroutine scope this inspector client runs in.
   *
   * Cancelling this scope has the side effect of disposing the inspector running on device. Likewise, exceptional events that happen to
   * the inspector (such as crash) will result in the cancellation of this scope. Therefore, calling join() on this scope is a reliable
   * way to find out when the inspector is disposed.
   */
  val scope: CoroutineScope
}

/**
 * A convenience function that awaits until the inspector is disposed.
 *
 * This method returns true if the messenger was disposed normally, or false if it was caused by a crash.
 */
suspend fun AppInspectorMessenger.awaitForDisposal(): Boolean {
  val job = scope.coroutineContext[Job]!!
  var crashed = false
  job.invokeOnCompletion { cause ->
    crashed = (cause is CancellationException && cause.cause is AppInspectionCrashException)
  }
  job.join()
  return !crashed
}
