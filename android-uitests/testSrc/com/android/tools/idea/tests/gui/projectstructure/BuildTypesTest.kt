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
package com.android.tools.idea.tests.gui.projectstructure

import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.tests.gui.framework.GuiTestRule
import com.android.tools.idea.tests.gui.framework.RunIn
import com.android.tools.idea.tests.gui.framework.TestGroup
import com.android.tools.idea.tests.gui.framework.fixture.newpsd.openPsd
import com.android.tools.idea.tests.gui.framework.fixture.newpsd.selectBuildVariantsConfigurable
import com.google.common.truth.Truth.assertThat
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GuiTestRemoteRunner::class)
class BuildTypesTest {

  @Rule
  @JvmField
  val guiTest = GuiTestRule()

  @Before
  fun setUp() {
    StudioFlags.NEW_PSD_ENABLED.override(true)
  }

  @After
  fun tearDown() {
    StudioFlags.NEW_PSD_ENABLED.clearOverride()
  }

  /**
   * Verifies that an existing build type can be updated.
   * <p>This is run to qualify releases. Please involve the test team in substantial changes.
   * <p>TT ID: 50840081-9584-4e66-9333-6a50902b5853
   * <pre>
   *   Test Steps:
   *   1. Open the project structure dialog
   *   2. Select the Build Variants view
   *   3. Click the Build Types tab
   *   4. Select Debug or Release and modify some settings.
   *   Verification:
   *   1. Build type selection in gradle build file is updated with the changes.
   * </pre>
   */
  @RunIn(TestGroup.FAST_BAZEL)
  @Test
  @Throws(Exception::class)
  fun editBuildType() {
    val ide = guiTest.importSimpleApplication()

    ide.openPsd().run {
      selectBuildVariantsConfigurable().run {
        selectBuildTypesTab().run {
          selectItemByPath("release")
          debuggable().selectItem("true")
          versionNameSuffix().enterText("suffix")
        }
      }
      clickOk()
    }

    val gradleFileContents = ide
      .editor
      .open("/app/build.gradle")
      .currentFileContents
    assertThat(gradleFileContents).containsMatch("release \\{\n[^\\}]* debuggable = true\n")
    assertThat(gradleFileContents).containsMatch("release \\{\n[^\\}]* versionNameSuffix = 'suffix'\n")
  }
}