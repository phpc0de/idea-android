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
package com.android.tools.idea.compose.preview.actions

import com.android.tools.idea.compose.preview.COMPOSE_PREVIEW_MANAGER
import com.android.tools.idea.compose.preview.isInStaticAndNonAnimationMode
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

private class ComposePreviewNonInteractiveActionWrapper(actions: List<AnAction>): DefaultActionGroup(actions) {
  override fun update(e: AnActionEvent) {
    super.update(e)

    e.getData(COMPOSE_PREVIEW_MANAGER)?.let {
      e.presentation.isVisible = it.isInStaticAndNonAnimationMode
    }
  }
}

/**
 * Makes the given list of actions only visible when the Compose preview is not in interactive or animation modes. Returns an [ActionGroup]
 * that handles the visibility.
 */
internal fun List<AnAction>.visibleOnlyInComposeStaticPreview(): ActionGroup = ComposePreviewNonInteractiveActionWrapper(this)

/**
 * Makes the given action only visible when the Compose preview is not in interactive or animation modes. Returns an [ActionGroup] that
 * handles the visibility.
 */
internal fun AnAction.visibleOnlyInComposeStaticPreview(): ActionGroup = listOf(this).visibleOnlyInComposeStaticPreview()