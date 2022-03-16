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
package com.android.tools.property.panel.impl.model

import com.android.SdkConstants
import com.android.tools.property.panel.api.PropertyItem

/**
 * Model for a boolean property: on/off (usually used for a single flag in a property with flags).
 */
class BooleanPropertyEditorModel(property: PropertyItem) : TextFieldWithLeftButtonEditorModel(property, true) {

  override var value: String
    get() = property.resolvedValue.orEmpty()
    set(value) { super.value = value }

  override fun toggleValue() {
    value = when (value) {
      SdkConstants.VALUE_TRUE -> SdkConstants.VALUE_FALSE
      else -> SdkConstants.VALUE_TRUE
    }
  }
}
