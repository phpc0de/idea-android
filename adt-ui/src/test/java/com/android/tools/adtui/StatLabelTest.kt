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
package com.android.tools.adtui

import com.android.tools.adtui.model.formatter.TimeFormatter
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatLabelTest {

  @Test
  fun `label display properly formatter number`() {
    val l = StatLabel(123456789, "description")
    assertThat(l.numText).isEqualTo("123,456,789")
    assertThat(l.descText).isEqualTo("description")
    l.numValue = 65432
    assertThat(l.numText).isEqualTo("65,432")
    assertThat(l.descText).isEqualTo("description") // (shouldn't be affected)
  }

  @Test
  fun customNumberFormatter() {
    val label = StatLabel(1000L, "Duration", numFormatter = TimeFormatter::getSingleUnitDurationString)
    assertThat(label.numText).isEqualTo("1 ms")
  }
}