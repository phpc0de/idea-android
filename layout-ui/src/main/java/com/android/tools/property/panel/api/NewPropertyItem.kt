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

import com.android.tools.adtui.model.stdui.EditingSupport

/**
 * Defines basic information about a property which name can be edited in a table.
 */
interface NewPropertyItem: PropertyItem {

  /**
   * The property name can be overridden.
   */
  override var name: String

  /**
   * The [PropertyItem] designated with the [namespace] and [name] of this new property.
   */
  val delegate: PropertyItem?
    get() = null

  /**
   * Editing support while editing this properties name.
   */
  val nameEditingSupport: EditingSupport
    get() = EditingSupport.INSTANCE

  /**
   * Return true if the qualified name is the same as the property specified.
   */
  fun isSameProperty(qualifiedName: String): Boolean
}
