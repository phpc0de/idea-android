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
package com.android.tools.idea.gradle.structure.model.android

import com.android.tools.idea.gradle.structure.model.PsProjectImpl
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.android.tools.idea.testing.TestProjectPaths
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat

class PsResolvedVariantCollectionTest : AndroidGradleTestCase() {

  fun testVariants() {
    loadProject(TestProjectPaths.PSD_SAMPLE_GROOVY)
    val resolvedProject = myFixture.project
    val project = PsProjectImpl(resolvedProject).also { it.testResolve() }

    val appModule = project.findModuleByGradlePath(":app") as PsAndroidModule

    assertThat(appModule.resolvedVariants.map { it.key }.toSet(), equalTo(setOf(
      PsVariantKey("debug", listOf("paid", "bar")),
      PsVariantKey("release", listOf("paid", "bar")),
      PsVariantKey("specialRelease", listOf("paid", "bar")),
      PsVariantKey("debug", listOf("paid", "otherBar")),
      PsVariantKey("release", listOf("paid", "otherBar")),
      PsVariantKey("specialRelease", listOf("paid", "otherBar")),
      PsVariantKey("debug", listOf("basic", "bar")),
      PsVariantKey("release", listOf("basic", "bar")),
      PsVariantKey("specialRelease", listOf("basic", "bar")),
      PsVariantKey("debug", listOf("basic", "otherBar")),
      PsVariantKey("release", listOf("basic", "otherBar")),
      PsVariantKey("specialRelease", listOf("basic", "otherBar"))
    )))
  }
}