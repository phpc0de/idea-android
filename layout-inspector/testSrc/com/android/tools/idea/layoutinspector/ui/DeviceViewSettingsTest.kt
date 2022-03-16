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
package com.android.tools.idea.layoutinspector.ui

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.RuleChain
import org.junit.Rule
import org.junit.Test

class DeviceViewSettingsTest {

  @get:Rule
  val ruleChain = RuleChain(ApplicationRule(), DeviceViewSettingsRule())

  @Test
  fun testSettingsPersisted() {
    val settings1 = DeviceViewSettings()
    settings1.drawBorders = true
    settings1.drawLabel = false

    assertThat(settings1.drawBorders).isTrue()
    assertThat(settings1.drawLabel).isFalse()

    val settings2 = DeviceViewSettings()
    assertThat(settings2.drawBorders).isTrue()
    assertThat(settings2.drawLabel).isFalse()

    settings2.drawBorders = false
    settings2.drawLabel = true

    assertThat(settings1.drawBorders).isFalse()
    assertThat(settings1.drawLabel).isTrue()
  }
}