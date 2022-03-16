/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.appinspection.inspectors.network.ide

import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.inspectors.common.api.ide.stacktrace.IntellijCodeNavigator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class CodeNavigationProviderTest {

  @get:Rule
  val projectRule = AndroidProjectRule.inMemory()

  @Test
  fun getCodeNavigator() {
    val navigator = DefaultCodeNavigationProvider(projectRule.project).codeNavigator
    assertThat(navigator).isInstanceOf(IntellijCodeNavigator::class.java)
  }
}