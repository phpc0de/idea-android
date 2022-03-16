/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run.tasks;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.ConsolePrinter;
import com.android.tools.idea.run.util.LaunchStatus;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class ShellCommandLauncher {
  private final static String TARGET_REGEX = "\\berror\\b";
  @VisibleForTesting
  static final Pattern errorPattern = Pattern.compile(TARGET_REGEX, Pattern.CASE_INSENSITIVE);

  public static boolean execute(@NotNull String command,
                                @NotNull IDevice device,
                                @NotNull LaunchStatus launchStatus,
                                @NotNull ConsolePrinter printer,
                                long timeout,
                                @NotNull TimeUnit timeoutUnit) {
    printer.stdout("$ adb shell " + command);

    CollectingOutputReceiver receiver = new AndroidLaunchReceiver(launchStatus);
    try {
      device.executeShellCommand(command, receiver, timeout, timeoutUnit);
    }
    catch (Exception e) {
      Logger logger = Logger.getInstance(ShellCommandLauncher.class);
      logger.warn("Unexpected exception while executing shell command: " + command);
      logger.warn(e);
      launchStatus.terminateLaunch("Unexpected error while executing: " + command, true);
      return false;
    }

    String output = receiver.getOutput();
    if (errorPattern.matcher(output).find()) {
      launchStatus.terminateLaunch("Error while executing: " + command, true);
      printer.stderr(output);
      return false;
    }

    return true;
  }

  private static class AndroidLaunchReceiver extends CollectingOutputReceiver {
    private final LaunchStatus myLaunchStatus;

    public AndroidLaunchReceiver(@NotNull LaunchStatus state) {
      myLaunchStatus = state;
    }

    @Override
    public boolean isCancelled() {
      return myLaunchStatus.isLaunchTerminated();
    }
  }
}
