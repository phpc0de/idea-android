/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.property.panel.api

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import javax.swing.Icon

/**
 * An action button consisting of an icon and an associated action.
 *
 * This interface is used for supplying a browse button to the right
 * of a property editor. See [PropertyItem.browseButton].
 */
interface ActionIconButton {
  /**
   * Return true if the action icon should be focusable.
   *
   * If [action] is not null this should be true to make the
   * button accessible from the keyboard.
   */
  val actionButtonFocusable: Boolean

  /**
   * Return the icon indicating the nature of this action button.
   */
  val actionIcon: Icon?

  /**
   * Return the action to be performed when the user activates the action button.
   *
   * If the action provided is an [ActionGroup] a menu will be shown instead.
   * An implementation may return null if the icon is for information purposes only.
   */
  val action: AnAction?
}
