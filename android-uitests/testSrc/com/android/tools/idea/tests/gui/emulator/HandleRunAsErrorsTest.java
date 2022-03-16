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
package com.android.tools.idea.tests.gui.emulator;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.RunIn;
import com.android.tools.idea.tests.gui.framework.TestGroup;
import com.android.tools.idea.tests.gui.framework.fixture.EditorFixture;
import com.android.tools.idea.tests.gui.framework.fixture.ExecutionToolWindowFixture;
import com.android.tools.idea.tests.gui.framework.fixture.IdeFrameFixture;
import com.android.tools.idea.tests.gui.framework.fixture.RunToolWindowFixture;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.fest.swing.util.PatternTextMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiTestRemoteRunner.class)
public class HandleRunAsErrorsTest {

  @Rule public final GuiTestRule guiTest = new GuiTestRule().withTimeout(5, TimeUnit.MINUTES);
  @Rule public final EmulatorTestRule emulator = new EmulatorTestRule();

  private static final String BEFORE = "setContentView(R.layout.activity_main);";
  private static final String AFTER = "";
  private static final String LINE1 = "int count = 1;";
  private static final String LINE2_COMMENTED = "//int size = 2;";
  private static final String TEXT = String.format("\n%s\n%s", LINE1, LINE2_COMMENTED);
  private static final String TEXT_REVERSE = String.format("\n%s\n%s", LINE2_COMMENTED, LINE1);
  private static final String REG_EXP = String.format("(.*%s.*\n.*%s.*)", LINE1, LINE2_COMMENTED);
  private static final String APP_NAME = "app";
  private static final Pattern RUN_CONNECTED_PATTERN = Pattern.
      compile(".*Connected to process.*", Pattern.DOTALL);
  private static final Pattern LAUNCH_APP_PATTERN = Pattern.
      compile(".*Launching 'app'.*", Pattern.DOTALL);

  /**
   * Handling of run-as errors: b.android.com/201285, b.android.com/201234, b.android.com/200881
   * <p>
   * This is run to qualify releases. Please involve the test team in substantial changes.
   * <p>
   * TT ID: fce32eb8-faf4-4023-bf24-195ed4ee6a57
   * <p>
   *   <pre>
   *   Test Steps:
   *   1. Import InstrumentationTest.
   *   2. Create an emulator.
   *   3. Add a two lines at onCreate() in MainActivity.java, make sure you comment second line.
   *   4. Run application on the emulator, wait for app is running on emulator.
   *   5. Swap above lines in MainActivity.java.
   *   6. Stop application.
   *   7. Run.
   *   Verification:
   *   1. Application should run successfully without any errors.
   *   </pre>
   */
  @RunIn(TestGroup.QA_UNRELIABLE) // b/77635374, fast
  @Test
  public void testHandleRunAsErrors() throws Exception {
    guiTest.importProjectAndWaitForProjectSyncToFinish("InstrumentationTest");
    emulator.createDefaultAVD(guiTest.ideFrame().invokeAvdManager());

    IdeFrameFixture ideFrame = guiTest.ideFrame();
    EditorFixture editor = ideFrame.getEditor();
    editor.open("app/src/main/java/google/instrumentationtest/MainActivity.java", EditorFixture.Tab.EDITOR)
        .moveBetween(BEFORE, AFTER)
        .enterText(TEXT);

    ideFrame.runApp(APP_NAME, emulator.getDefaultAvdName());

    // Wait for app is running.
    RunToolWindowFixture runToolWindowFixture = new RunToolWindowFixture(ideFrame);
    waitForSessionStart(runToolWindowFixture);

    editor.moveBetween(BEFORE, AFTER)
        .select(REG_EXP)
        .enterText(TEXT_REVERSE);

    ideFrame.stopApp();

    ideFrame.runApp(APP_NAME, emulator.getDefaultAvdName());

    // TODO: Find the reason why emulator is disconnected without waiting, and do it in the right way.
    // Workaround: Wait to skip the scenario encountering emulator disconnected.
    Thread.sleep(10000);

    waitForSessionStart(runToolWindowFixture);
  }

  private static void waitForSessionStart(@NotNull RunToolWindowFixture runToolWindowFixture) {
    ExecutionToolWindowFixture.ContentFixture contentFixture = runToolWindowFixture
        .findContent(APP_NAME);
    contentFixture.waitForOutput(new PatternTextMatcher(LAUNCH_APP_PATTERN), 20);
    contentFixture.waitForOutput(new PatternTextMatcher(RUN_CONNECTED_PATTERN), 120);
  }
}
