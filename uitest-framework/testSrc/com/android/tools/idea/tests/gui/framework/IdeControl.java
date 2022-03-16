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
package com.android.tools.idea.tests.gui.framework;

import com.intellij.openapi.wm.IdeFrame;
import com.intellij.testGuiFramework.impl.GuiTestThread;
import com.intellij.testGuiFramework.remote.client.JUnitClient;
import com.intellij.testGuiFramework.remote.transport.RestartIdeMessage;

import java.util.function.Supplier;

import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.finder.WindowFinder;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class IdeControl extends TestWatcher {

  public static RestartIdeMessage restartMessage = null;

  private Supplier<Robot> robotSupplier;

  public IdeControl(Supplier<Robot> robotSupplier) {
    this.robotSupplier = robotSupplier;
  }

  /**
   * This test rule ensures that the IDE has initialized fully before proceeding with a test.
   */
  @Override
  public void starting(Description description) {
    Robot robot = robotSupplier.get();
    GuiActionRunner.executeInEDT(false);
    try {
      WindowFinder.findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
        @Override
        protected boolean isMatching(@NotNull Frame frame) {
          return (frame instanceof IdeFrame && frame.isShowing());
        }
      }).withTimeout(TimeUnit.MINUTES.toMillis(1)).using(robot);
    } finally {
      GuiActionRunner.executeInEDT(true);
    }
  }

 /**
  * For tests that restart the IDE and resume afterwards, the client sends a message to the server requesting a restart. The server responds
  * with a CLOSE_IDE message. However, if the test sent the request itself, the IDE would close before all of the framework code that runs
  * after a test got a chance to run. Instead the test just sets restartMessage, and we actually send the message here.
  */
  @Override
  public void succeeded(Description description) {
    if (restartMessage != null) {
      JUnitClient client = GuiTestThread.Companion.getClient();
      if (client != null) {
        client.send(restartMessage);
      }
    }
  }
}