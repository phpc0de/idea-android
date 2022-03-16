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
package com.android.tools.idea.uibuilder.handlers.compose

import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.common.scene.SceneInteraction
import com.android.tools.idea.common.surface.Interaction
import com.android.tools.idea.uibuilder.api.ViewGroupHandler
import com.android.tools.idea.uibuilder.surface.ScreenView

/**
 * [ViewGroupHandler] for the `ComposeViewAdapter`. It disables all interactions with the component since
 * Compose elements can not be interacted with.
 */
class ComposeViewAdapterHandler: ViewGroupHandler() {
  override fun acceptsChild(layout: NlComponent, newChild: NlComponent) = false

  override fun createInteraction(screenView: ScreenView, x: Int, y: Int, component: NlComponent): Interaction? =
    SceneInteraction(screenView)
}