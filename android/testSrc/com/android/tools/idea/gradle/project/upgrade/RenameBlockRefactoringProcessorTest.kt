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
package com.android.tools.idea.gradle.project.upgrade

import com.android.ide.common.repository.GradleVersion
import com.android.tools.idea.gradle.project.upgrade.AgpUpgradeComponentNecessity.*
import com.google.common.truth.Expect
import com.intellij.openapi.project.Project
import com.intellij.testFramework.RunsInEdt
import org.junit.Rule
import org.junit.Test

@RunsInEdt
class RenameBlockRefactoringProcessorTest: UpgradeGradleFileModelTestCase() {
  @get:Rule
  val expect: Expect = Expect.createAndEnableStackTrace()

  private fun aaptOptionsToAndroidResourcesRefactoringProcessor(project: Project, current: GradleVersion, new: GradleVersion) =
    MIGRATE_AAPT_OPTIONS_TO_ANDROID_RESOURCES.RefactoringProcessor(project, current, new)

  @Test
  fun testNecessities() {
    val expectedNecessitiesMap = mapOf(
      ("4.1.0" to "4.2.0") to IRRELEVANT_FUTURE,
      ("4.2.0" to "7.0.0") to OPTIONAL_CODEPENDENT,
      ("7.0.0" to "7.1.0") to OPTIONAL_INDEPENDENT,
      ("7.1.0" to "8.0.0") to MANDATORY_INDEPENDENT,
      ("4.2.0" to "8.0.0") to MANDATORY_CODEPENDENT,
      ("8.0.0" to "8.1.0") to IRRELEVANT_PAST
    )
    expectedNecessitiesMap.forEach { (t, u) ->
      val processor = aaptOptionsToAndroidResourcesRefactoringProcessor(project, GradleVersion.parse(t.first), GradleVersion.parse(t.second))
      expect.that(processor.necessity()).isEqualTo(u)
    }
  }

  @Test
  fun testAaptOptionsToAndroidResources() {
    writeToBuildFile(TestFileName("RenameBlocks/AaptOptionsToAndroidResources"))
    val processor = aaptOptionsToAndroidResourcesRefactoringProcessor(project, GradleVersion.parse("7.0.0"), GradleVersion.parse("8.0.0"))
    processor.run()
    verifyFileContents(buildFile, TestFileName("RenameBlocks/AaptOptionsToAndroidResourcesExpected"))
  }
}