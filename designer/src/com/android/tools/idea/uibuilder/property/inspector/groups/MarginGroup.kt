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
package com.android.tools.idea.uibuilder.property.inspector.groups

import com.android.SdkConstants
import com.android.tools.property.panel.api.PropertiesTable
import com.android.tools.idea.uibuilder.property.NlPropertyItem

class MarginGroup(properties: PropertiesTable<NlPropertyItem>) :
  AbstractMarginGroup("layout_margin",
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_LEFT),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_START),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_END),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_TOP),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_HORIZONTAL),
                      properties.getOrNull(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_MARGIN_VERTICAL))
