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
package com.android.tools.property.panel.impl.model.util

import com.android.tools.property.ptable2.PTableItem
import com.android.tools.property.ptable2.PTableModelUpdateListener

class FakePTableModelUpdateListener : PTableModelUpdateListener {
  var updateCount = 0
  var nextEditedItem: PTableItem? = null
  var modelWasChanged = false

  override fun itemsUpdated(modelChanged: Boolean, nextEditedItem: PTableItem?) {
    this.nextEditedItem = nextEditedItem
    modelWasChanged = modelWasChanged or modelChanged
    updateCount++
  }
}
