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
package com.android.tools.idea.uibuilder.actions.analytics

import com.android.tools.idea.assistant.AssistNavListener
import com.android.tools.idea.common.assistant.AssistantPanelMetricsTracker
import com.google.wireless.android.sdk.stats.DesignEditorHelpPanelEvent.HelpPanelType
import java.awt.event.ActionEvent

/**
 * Assistant Panel navigation listener. It listens to navigation events
 * specific to the layout editor (filtered through [AssistantPanelNavListener.getIdPrefix])
 *
 * For now, it listens to all events triggered from [HelpPanelType.FULL_ALL]
 */
class AssistantPanelNavListener : AssistNavListener {
  override fun getIdPrefix(): String {
    return "analytics-layouteditor"
  }

  override fun onActionPerformed(id: String?, e: ActionEvent) {
    AssistantPanelMetricsTracker(HelpPanelType.FULL_ALL).logButtonClicked()
  }
}
