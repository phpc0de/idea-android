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
package com.android.tools.idea.run.deployment;

import com.android.ddmlib.IDevice;
import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.run.AndroidDevice;
import com.android.tools.idea.run.LaunchCompatibility;
import com.android.tools.idea.run.deployable.Deployable;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Device {
  @NotNull
  private final String myName;

  private final @NotNull Type myType;

  @NotNull
  private final LaunchCompatibility myLaunchCompatibility;

  @NotNull
  private final Key myKey;

  @Nullable
  private final Instant myConnectionTime;

  @NotNull
  private final AndroidDevice myAndroidDevice;

  static abstract class Builder {
    @Nullable
    String myName;

    @Nullable LaunchCompatibility myLaunchCompatibility;

    @Nullable
    Key myKey;

    @Nullable
    Instant myConnectionTime;

    @Nullable
    AndroidDevice myAndroidDevice;

    @Nullable Type myType;

    Builder() {
      myLaunchCompatibility = LaunchCompatibility.YES;
    }

    @NotNull
    abstract Device build();
  }

  Device(@NotNull Builder builder) {
    assert builder.myName != null;
    myName = builder.myName;
    myType = builder.myType;

    myLaunchCompatibility = builder.myLaunchCompatibility;

    assert builder.myKey != null;
    myKey = builder.myKey;

    myConnectionTime = builder.myConnectionTime;

    assert builder.myAndroidDevice != null;
    myAndroidDevice = builder.myAndroidDevice;
  }

  final @NotNull LaunchCompatibility getLaunchCompatibility() {
    return myLaunchCompatibility;
  }

  @NotNull
  abstract Icon getIcon();

  abstract boolean isConnected();

  @NotNull
  public final String getName() {
    return myName;
  }

  abstract @NotNull Collection<@NotNull Snapshot> getSnapshots();

  /**
   * A physical device will always return a serial number. A virtual device will usually return a virtual device path. But if Studio doesn't
   * know about the virtual device (it's outside the scope of the AVD Manager because it uses a locally built system image, for example) it
   * can return a virtual device path (probably not but I'm not going to assume), virtual device name, or serial number depending on what
   * the IDevice returned.
   */
  @NotNull
  public final Key getKey() {
    return myKey;
  }

  @Nullable
  final Instant getConnectionTime() {
    return myConnectionTime;
  }

  abstract @NotNull Target getDefaultTarget();

  abstract @NotNull Collection<@NotNull Target> getTargets();

  @NotNull
  final AndroidDevice getAndroidDevice() {
    return myAndroidDevice;
  }

  @NotNull
  abstract Future<AndroidVersion> getAndroidVersion();

  final boolean isRunning(@NotNull String appPackage) {
    if (!isConnected()) {
      return false;
    }

    IDevice device = getDdmlibDevice();
    assert device != null;

    if (!device.isOnline()) {
      return false;
    }

    return !Deployable.searchClientsForPackage(device, appPackage).isEmpty();
  }

  @Nullable
  final IDevice getDdmlibDevice() {
    AndroidDevice device = getAndroidDevice();

    if (!device.isRunning()) {
      return null;
    }

    try {
      return device.getLaunchedDevice().get();
    }
    catch (InterruptedException | ExecutionException exception) {
      throw new AssertionError(exception);
    }
  }

  @NotNull
  @Override
  public final String toString() {
    return myName;
  }

  @NotNull Type getType() {
    return myType;
  }

  enum Type {
    PHONE,
    WEAR,
    TV
  }
}
