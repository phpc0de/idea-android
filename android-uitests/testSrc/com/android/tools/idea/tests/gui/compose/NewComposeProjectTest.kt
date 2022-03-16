/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.compose

import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.tests.gui.framework.GuiTestRule
import com.android.tools.idea.tests.util.WizardUtils
import com.android.tools.idea.wizard.template.Language
import com.google.common.truth.Truth.assertThat
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(GuiTestRemoteRunner::class)
class NewComposeProjectTest {
  @get:Rule
  val guiTest = GuiTestRule().withTimeout(5, TimeUnit.MINUTES)

  @Before
  fun setup() {
    StudioFlags.COMPOSE_WIZARD_TEMPLATES.override(true)
  }

  @After
  fun cleanUp() {
    StudioFlags.COMPOSE_WIZARD_TEMPLATES.clearOverride()
  }

  /**
   * Verifies that user is able to create a new Compose Activity Project through the
   * new project wizard.
   * <p>TT ID: f1c58981-0704-40be-9794-7f61e425a8d5
   * Test steps:
   * 1. Create new default "Empty Compose Activity" Project
   * Verify:
   * 1. Check that app/build.gradle has dependencies for "androidx.compose.ui:ui-framework" and "androidx.compose.ui:ui-tooling"
   * 2. Check that the main activity has functions annotated with @Composable and @Preview
   * 3. Check Gradle Sync to success
   */
  @Test
  fun newComposeProject() {
    WizardUtils.createNewProject(guiTest, "Empty Compose Activity", Language.Kotlin)

    guiTest.getProjectFileText("app/build.gradle").run {
      assertThat(this).contains("implementation \"androidx.compose.ui:ui:")
      assertThat(this).contains("implementation \"androidx.compose.material:material:")
      assertThat(this).contains("implementation \"androidx.compose.ui:ui-tooling-preview:")
      assertThat(this).contains("debugImplementation \"androidx.compose.ui:ui-tooling:")
    }
    guiTest.getProjectFileText("app/src/main/java/com/google/myapplication/MainActivity.kt").run {
      assertThat(this).contains("@Composable")
      assertThat(this).contains("@Preview")
    }

    guiTest.ideFrame().requestProjectSyncAndWaitForSyncToFinish()
  }
}