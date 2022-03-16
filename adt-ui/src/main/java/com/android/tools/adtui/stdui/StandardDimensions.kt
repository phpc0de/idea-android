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
package com.android.tools.adtui.stdui

import com.intellij.util.ui.JBUI

/**
 * Standard UI Component Dimensions.
 */
object StandardDimensions {
  const val OUTER_BORDER_UNSCALED = 2

  val COMBO_LEFT_PADDING = JBUI.scale(7)
  val HORIZONTAL_PADDING = JBUI.scale(6)
  val VERTICAL_PADDING = JBUI.scale(1)
  val INNER_BORDER_WIDTH = JBUI.scale(1f)
  val OUTER_BORDER_WIDTH = JBUI.scale(OUTER_BORDER_UNSCALED.toFloat())
  val DROPDOWN_ARROW_WIDTH = JBUI.scale(8f)
  val DROPDOWN_ARROW_HEIGHT = JBUI.scale(5f)
  val DROPDOWN_ARROW_HORIZONTAL_PADDING = JBUI.scale(4f)
  val DROPDOWN_ARROW_VERTICAL_PADDING_TOP = JBUI.scale(7f)
  val DROPDOWN_ARROW_VERTICAL_PADDING_BOTTOM = JBUI.scale(6f)
  var MENU_HEIGHT = JBUI.scale(20f)
  var MENU_LEFT_PADDING = JBUI.scale(6f)
  var MENU_RIGHT_PADDING = JBUI.scale(10f)
  var MENU_ICON_TEXT_GAP = JBUI.scale(4f)
  var MENU_CHECK_ICON_GAP = JBUI.scale(5f)
}
