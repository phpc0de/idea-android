/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.debugger;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.RunIn;
import com.android.tools.idea.tests.gui.framework.TestGroup;
import com.android.tools.idea.tests.gui.framework.emulator.AvdSpec;
import com.android.tools.idea.tests.gui.framework.emulator.AvdTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.DebugToolWindowFixture;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(GuiTestRemoteRunner.class)
public class WatchpointTest extends DebuggerTestBase {
  private final GuiTestRule guiTest = new GuiTestRule().withTimeout(10, TimeUnit.MINUTES);
  private final AvdTestRule avdRule = AvdTestRule.Companion.buildAvdTestRule(() ->
    new AvdSpec.Builder()
  );
  @Rule public final RuleChain emulatorRules = RuleChain
    .outerRule(avdRule)
    .around(guiTest);

  @Before
  public void setupSpecialSdk() {
    DebuggerTestUtil.setupSpecialSdk(avdRule);
  }

  @Before
  public void symlinkLldb() throws IOException {
    DebuggerTestUtil.symlinkLldb();
  }

  /**
   * Verifies that debugger stops an app once watched variable is read and/or written.
   * <p>
   * This is run to qualify releases. Please involve the test team in substantial changes.
   * <p>
   * TT ID: 3405822b-b83d-4cb8-be79-ea27454912a4
   * <p>
   *   <pre>
   *   Test Steps:
   *   1. Import BasicJniApp.
   *   2. Create an emulator.
   *   3. Set a breakpoint at the dummy variable definition in C++ code.
   *   4. Debug on the emulator.
   *   5. When the C++ breakpoint is hit, verify variables.
   *   6. In Debug window, right click on the variable and click Add Watchpoint.
   *   7. Resume program, and verify that that debugger stops an app once watched variable is read and/or written.
   *   </pre>
   */
  @Test
  @RunIn(TestGroup.FAST_BAZEL)
  public void testWatchpoint() throws Exception {
    guiTest.importProjectAndWaitForProjectSyncToFinish("debugger/WatchpointTestAppForUI");

    final IdeFrameFixture ideFrame = guiTest.ideFrame();

    DebuggerTestUtil.setDebuggerType(ideFrame, DebuggerTestUtil.NATIVE);

    // Setup breakpoints
    openAndToggleBreakPoints(ideFrame, "app/src/main/jni/native-lib.c", "int dummy = 1;");

    // Setup the expected patterns to match the variable values displayed in Debug windows's 'Variables' tab.
    String[] expectedPattern = {variableToSearchPattern("write", "int", "5")};

    DebugToolWindowFixture debugToolWindowFixture =
      DebuggerTestUtil.debugAppAndWaitForSessionToStart(ideFrame, guiTest, DEBUG_CONFIG_NAME, avdRule.getMyAvd().getName());
    checkAppIsPaused(ideFrame, expectedPattern);

    DebugToolWindowFixture.ContentFixture contentFixture = debugToolWindowFixture.findContent(DEBUG_CONFIG_NAME);

    JBPopupMenu popupMenu = contentFixture.rightClickVariableInDebuggerVariables(ideFrame, "write");
    contentFixture.findWatchpointConfig(ideFrame, popupMenu)
        .selectAccessType("Write")
        .clickDone();

    resume("app", ideFrame);

    String[] newExpectedPattern = {variableToSearchPattern("write", "int", "8")};
    checkAppIsPaused(ideFrame, newExpectedPattern);

    stopDebugSession(debugToolWindowFixture);
  }
}
