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
package com.android.tools.idea.tests.gui.uibuilder;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.RunIn;
import com.android.tools.idea.tests.gui.framework.TestGroup;
import com.android.tools.idea.tests.gui.framework.fixture.assetstudio.AssetStudioWizardFixture;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.fest.swing.core.MouseButton;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(GuiTestRemoteRunner.class)
public class ImageAssetErrorCheckTest {

  @Rule public final GuiTestRule guiTest = new GuiTestRule().withTimeout(5, TimeUnit.MINUTES);
  @Rule public final RenderTaskLeakCheckRule renderTaskLeakCheckRule = new RenderTaskLeakCheckRule();

  /**
   * Verifies that SVG images can be loaded in Asset Studio
   * <p>
   * This is run to qualify releases. Please involve the test team in substantial changes.
   * <p>
   * TT ID: 5b7ac387-09b9-41a3-85cf-1f89659c65aa
   * <p>
   *   <pre>
   *   Test Steps:
   *   1. Open Android Studio
   *   2. Import SimpleApplication
   *   3. Right click the default app module (or any manually created module) and select New > Vector Asset (Verify)
   *   4. Select Local SVG file option
   *   5. Choose an image file from the local system
   *   Verify:
   *   For supported files, conversion to svg should succeed
   *   For unsupported files, “Errors” dialog should display at the bottom of Configure vector asset window with appropriate message
   *   </pre>
   */
  @RunIn(TestGroup.FAST_BAZEL)
  @Test
  public void imageAssetErrorCheck() throws Exception {
    guiTest.importSimpleApplication()
      .getProjectView()
      .selectAndroidPane()
      .clickPath(MouseButton.RIGHT_BUTTON, "app")
      .openFromContextualMenu(AssetStudioWizardFixture::find, "New", "Vector Asset")
      .useLocalFile(GuiTests.getTestDataDir() + "/TestImages/call.svg")
      .waitUntilStepErrorMessageIsGone()
      .useLocalFile(GuiTests.getTestDataDir() + "/TestImages/android_wrong.svg")
      .assertStepErrorMessage(errorMsg -> errorMsg.contains("Error while parsing android_wrong.svg"))
      .clickCancel();
  }
}
