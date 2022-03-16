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
package com.android.tools.idea.tests.util.ddmlib;

import com.android.SdkConstants;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.tools.idea.adb.AdbService;
import com.android.tools.idea.avdmanager.AvdManagerConnection;
import com.android.tools.idea.sdk.AndroidSdks;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import java.io.File;
import java.util.List;
import org.fest.swing.timing.Wait;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidDebugBridgeUtils {
  @NotNull
  public static AndroidDebugBridge getAdb() {
    File adbBinary =
      AndroidSdks.getInstance()
        .tryToChooseSdkHandler()
        .getLocation()
        .resolve(SdkConstants.OS_SDK_PLATFORM_TOOLS_FOLDER)
        .resolve(SdkConstants.FN_ADB)
        .toFile();
    return AndroidDebugBridge.createBridge(adbBinary.getAbsolutePath(), false);
  }

  @Nullable
  public static IDevice getEmulator(@NotNull String avdName, @NotNull AvdManagerConnection avdManagerConnection, long timeoutInSeconds) {
    AndroidDebugBridge adb = getAdb();
    List<AvdInfo> avds = avdManagerConnection.getAvds(false);

    Wait.seconds(timeoutInSeconds)
      .expecting("Emulator device with name " + avdName + " to appear")
      .until(() -> getEmulatorDevice(avdName, avds, adb.getDevices()) != null);

    return getEmulatorDevice(avdName, avds, adb.getDevices());
  }

  @Nullable
  private static IDevice getEmulatorDevice(@NotNull String avdName, @NotNull List<AvdInfo> avds, @NotNull IDevice[] deviceList) {
    for (IDevice dev : deviceList) {
      if (dev.isEmulator() && doesEmulatorHaveName(avdName, avds, dev)) {
        return dev;
      }
    }
    return null;
  }

  private static boolean doesEmulatorHaveName(@NotNull String avdName, @NotNull List<AvdInfo> avds, @NotNull IDevice device) {
    for (AvdInfo avd : avds) {
      if (avd.getName().equals(device.getAvdName())
          && avdName.equals(avd.getDisplayName())) {
        return true;
      }
    }
    return false;
  }

  public static void enableFakeAdbServerMode(int port) {
    // Terminate the service if it's already started (it's a UI test, so there might be no shutdown between tests).
    Disposer.dispose(AdbService.getInstance());
    // Enable the fake ADB server and attach a fake device to which the preview will be deployed.
    AndroidDebugBridge.enableFakeAdbServerMode(port);
  }
}
