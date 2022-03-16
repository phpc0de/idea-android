/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.util.function.Supplier;
import org.fest.swing.core.Robot;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;

/** Rule that takes a screenshot when the test fails. */
class ScreenshotOnFailure extends TestWatcher {

  private final @NotNull Supplier<Robot> myRobotSupplier;

  public ScreenshotOnFailure(@NotNull Supplier<Robot> robotSupplier) {
    myRobotSupplier = robotSupplier;
  }

  @Override
  protected void failed(Throwable throwable, Description description) {
    String fileName = description.getTestClass().getSimpleName() + "." + description.getMethodName() + ".png";

    try {
      File file = new File(GuiTests.getFailedTestScreenshotDirPath(), fileName);
      file.delete();
      new ScreenshotTaker(myRobotSupplier.get()).saveDesktopAsPng(file.getPath());
      System.out.println("Screenshot: " + file);
    }
    catch (Throwable t) {
      System.out.println("Screenshot failed. " + t.getMessage());
      t.printStackTrace();
    }
  }
}
