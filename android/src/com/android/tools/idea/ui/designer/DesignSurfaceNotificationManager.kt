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
package com.android.tools.idea.ui.designer

import com.android.tools.idea.rendering.RenderUtils
import com.intellij.openapi.actionSystem.DataKey

/** Control for notification created through EditorNotification that spans over DesignSurface. */
interface DesignSurfaceNotificationManager {

  /** show notification with given text. */
  fun showNotification(text: String)

  /** show and hide notification after [timems]*/
  fun showThenHideNotification(text: String, timems: Int)

  /** hide the notification if it was showing. No-op otherwise. */
  fun hideNotification()
}

/** Data key to retrieve [DesignSurfaceNotificationManager] which controls the notification status of [EditorDesignSurface] */
@JvmField
val NOTIFICATION_KEY = DataKey.create<DesignSurfaceNotificationManager>(DesignSurfaceNotificationManager::class.java.name)
