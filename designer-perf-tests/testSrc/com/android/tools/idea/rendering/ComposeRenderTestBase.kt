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
package com.android.tools.idea.rendering

import com.android.testutils.TestUtils
import com.android.tools.idea.testing.AndroidGradleProjectRule
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.android.uipreview.ModuleClassLoaderManager
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule


open class ComposeRenderTestBase {
  @get:Rule
  val projectRule = AndroidGradleProjectRule()

  @Before
  open fun setUp() {
    RenderTestUtil.beforeRenderTestCase()
    RenderService.setForTesting(projectRule.project, NoSecurityManagerRenderService(projectRule.project))
    val baseTestPath = TestUtils.resolveWorkspacePath("tools/adt/idea/designer-perf-tests/testData").toString()
    projectRule.fixture.testDataPath = baseTestPath
    projectRule.load(SIMPLE_COMPOSE_PROJECT_PATH, kotlinVersion = "1.5.10")
    projectRule.requestSyncAndWait()

    projectRule.invokeTasks("compileDebugSources").apply {
      buildError?.printStackTrace()
      Assert.assertTrue("The project must compile correctly for the test to pass", isBuildSuccessful)
    }

    ModuleClassLoaderManager.get().setCaptureClassLoadingDiagnostics(true)
  }

  @After
  open fun tearDown() {
    ModuleClassLoaderManager.get().setCaptureClassLoadingDiagnostics(false)
    ApplicationManager.getApplication().invokeAndWait {
      RenderTestUtil.afterRenderTestCase()
    }
    RenderService.setForTesting(projectRule.project, null)
  }
}