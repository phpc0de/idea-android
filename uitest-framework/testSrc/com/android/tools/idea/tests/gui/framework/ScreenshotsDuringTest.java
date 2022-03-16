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

import com.intellij.openapi.util.io.FileUtilRt;
import java.util.function.Supplier;
import org.fest.swing.core.Robot;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.intellij.openapi.util.io.FileUtil.ensureExists;

/**
 * Rule that takes a screenshot at certain intervals (default 100ms)
 */
public class ScreenshotsDuringTest extends TestWatcher {
  private final @NotNull Supplier<Robot> myRobotSupplier;
  private final @NotNull ScheduledExecutorService myExecutorService;
  private final int myPeriod;
  private File myFolder;

  public ScreenshotsDuringTest(@NotNull Supplier<Robot> robotSupplier) {
    this(robotSupplier, 100);
  }

  /**
   * <p>WARNING: Do not set {@code period} to too small a value. The
   * ScreenshotTaker may take screenshots by posting events to the EDT,
   * which can prevent the robot from ever reaching an "idle" state.</p>
   *
   * @param period time to wait between screenshots in milliseconds
   */
  public ScreenshotsDuringTest(@NotNull Supplier<Robot> robotSupplier, int period) {
    myRobotSupplier = robotSupplier;
    myPeriod = period;
    myExecutorService = Executors.newScheduledThreadPool(1);
  }

  @Override
  protected void starting(Description description) {
    String folderName = description.getTestClass().getSimpleName() + "-" + description.getMethodName();
    try {
      myFolder = new File(GuiTests.getFailedTestScreenshotDirPath(), folderName);
      ensureExists(myFolder);
    }
    catch (IOException e) {
      System.out.println("Could not create folder " + folderName);
    }

    ScreenshotTaker screenshotTaker = new ScreenshotTaker(myRobotSupplier.get());
    myExecutorService.scheduleAtFixedRate(() -> {
      try {
        screenshotTaker.saveDesktopAsPng(new File(myFolder, System.currentTimeMillis() + ".png").getPath());
      }
      catch (Throwable e) {
        // Do nothing
      }
    }, 100, myPeriod, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void finished(Description description) {
    myExecutorService.shutdown();
    try {
      myExecutorService.awaitTermination(myPeriod, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      // Do not report the timeout
    }
  }

  @Override
  protected void succeeded(Description description) {
    if (myFolder != null) {
      FileUtilRt.delete(myFolder);
    }
  }
}
