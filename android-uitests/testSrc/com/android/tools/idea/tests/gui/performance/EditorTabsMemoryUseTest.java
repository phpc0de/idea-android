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
package com.android.tools.idea.tests.gui.performance;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.RunIn;
import com.android.tools.idea.tests.gui.framework.TestGroup;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.bleak.UseBleak;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Checks for leaks of data associated with editor tabs. One tab is kept open to ensure that
 * higher-level structures aren't reclaimed, potentially taking everything of interest along with them.
 */
@RunWith(GuiTestRemoteRunner.class)
@RunIn(TestGroup.PERFORMANCE)
public class EditorTabsMemoryUseTest {

  @Rule public final GuiTestRule guiTest = new GuiTestRule();

  @Test
  @UseBleak
  public void openAndCloseTabs() throws Exception {
    IdeFrameFixture ideFrameFixture = guiTest.importSimpleApplication();
    ideFrameFixture.getEditor().open("app/src/main/java/google/simpleapplication/MyActivity.java");
    guiTest.runWithBleak(() -> {
      ideFrameFixture.getEditor()
        .open("app/src/main/AndroidManifest.xml")
        .open("app/build.gradle")
        .close()
        .close();
    });
  }

}
