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
package com.android.tools.idea.navigator

import com.android.tools.idea.testing.TestProjectToSnapshotPaths
import com.intellij.testFramework.RunsInEdt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunsInEdt
@RunWith(Parameterized::class)
class AndroidProjectViewNodeConsistencyTest : AndroidProjectViewNodeConsistencyTestBase() {

  companion object {
    @Suppress("unused")
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun testProjects(): Collection<*> = listOf(
      TestProject(TestProjectToSnapshotPaths.PSD_SAMPLE_GROOVY),
      TestProject(TestProjectToSnapshotPaths.COMPOSITE_BUILD),
      TestProject(TestProjectToSnapshotPaths.NON_STANDARD_SOURCE_SETS, "/application"),
      TestProject(TestProjectToSnapshotPaths.LINKED, "/firstapp")
    )
  }

  @Test
  fun testContains() = super.testContainsImpl()

  @Test
  fun testCanRepresent() = super.testCanRepresentImpl()
}