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

import com.android.ddmlib.IDevice;
import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.run.ConsolePrinter;
import com.android.tools.idea.run.activity.StartActivityFlagsProvider;
import com.android.tools.idea.run.util.LaunchStatus;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.EventCategory;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.EventKind;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class AndroidDeepLinkLaunchTask extends AppLaunchTask {

  private static final String ID = "LAUNCH_DEEP_LINK";

  @NotNull private final String myDeepLink;
  @NotNull StartActivityFlagsProvider myStartActivityFlagsProvider;

  public AndroidDeepLinkLaunchTask(@NotNull String deepLink,
                                   @NotNull StartActivityFlagsProvider startActivityFlagsProvider) {
    myDeepLink = deepLink;
    myStartActivityFlagsProvider = startActivityFlagsProvider;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Launching URL";
  }

  @Override
  public int getDuration() {
    return LaunchTaskDurations.LAUNCH_ACTIVITY;
  }

  @Override
  public LaunchResult run(@NotNull LaunchContext launchContext) {
    ConsolePrinter printer = launchContext.getConsolePrinter();
    IDevice device = launchContext.getDevice();
    LaunchStatus launchStatus = launchContext.getLaunchStatus();

    printer.stdout("Launching deeplink: " + myDeepLink + ".\n");
    UsageTracker.log(AndroidStudioEvent.newBuilder()
                                   .setCategory(EventCategory.APP_INDEXING)
                                   .setKind(EventKind.APP_INDEXING_DEEP_LINK_LAUNCHED));
    // Enable AppIndexing API log
    ShellCommandLauncher.execute("setprop log.tag.AppIndexApi VERBOSE", device, launchStatus, printer, 5, TimeUnit.SECONDS);

    // Launch deeplink
    String command = getLaunchDeepLinkCommand(myDeepLink, myStartActivityFlagsProvider.getFlags(device));
    boolean successful = ShellCommandLauncher.execute(command, device, launchStatus, printer, 5, TimeUnit.SECONDS);
    return successful ? LaunchResult.success() : LaunchResult.error("", getDescription());
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @NotNull
  public static String getLaunchDeepLinkCommand(@NotNull String deepLink,
                                                @NotNull String extraFlags) {
    return "am start" +
           " -a android.intent.action.VIEW" +
           " -c android.intent.category.BROWSABLE" +
           " -d " + singleQuoteShell(deepLink) +
           (extraFlags.isEmpty() ? "" : " " + extraFlags);
  }

  @NotNull
  private static String singleQuoteShell(@NotNull String literal) {
    return "'" + literal.replace("'", "'\\''") + "'";
  }

}
