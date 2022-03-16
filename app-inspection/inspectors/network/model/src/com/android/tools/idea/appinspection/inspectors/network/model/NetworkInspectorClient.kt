/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.appinspection.inspectors.network.model

import com.android.tools.idea.appinspection.inspector.api.AppInspectorMessenger
import studio.network.inspection.NetworkInspectorProtocol.Command
import studio.network.inspection.NetworkInspectorProtocol.Response
import studio.network.inspection.NetworkInspectorProtocol.StartInspectionCommand

class NetworkInspectorClient(
  private val messenger: AppInspectorMessenger
) {
  suspend fun getStartTimeStampNs(): Long {
    val response = messenger.sendRawCommand {
      startInspectionCommand = StartInspectionCommand.getDefaultInstance()
    }
    return response.startInspectionResponse.timestamp
  }
}

private suspend fun AppInspectorMessenger.sendRawCommand(init: Command.Builder.() -> Unit): Response {
  val response = sendRawCommand(Command.newBuilder().also(init).build().toByteArray())
  return Response.parseFrom(response)
}