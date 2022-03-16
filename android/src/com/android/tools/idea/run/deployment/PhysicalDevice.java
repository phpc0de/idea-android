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

import static icons.StudioIcons.Common.ERROR_DECORATOR;

import com.android.ddmlib.IDevice;
import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.run.AndroidDevice;
import com.android.tools.idea.run.DeploymentApplicationService;
import com.android.tools.idea.run.LaunchCompatibility;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import icons.StudioIcons;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Function;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PhysicalDevice extends Device {
  private static final Icon ourPhoneIcon = ExecutionUtil.getLiveIndicator(StudioIcons.DeviceExplorer.PHYSICAL_DEVICE_PHONE);
  private static final Icon ourWearIcon = ExecutionUtil.getLiveIndicator(StudioIcons.DeviceExplorer.PHYSICAL_DEVICE_WEAR);
  private static final Icon ourTvIcon = ExecutionUtil.getLiveIndicator(StudioIcons.DeviceExplorer.PHYSICAL_DEVICE_TV);

  @NotNull
  static PhysicalDevice newDevice(@NotNull ConnectedDevice device,
                                  @NotNull Function<ConnectedDevice, String> getName,
                                  @NotNull KeyToConnectionTimeMap map) {
    Key key = device.getKey();

    return new Builder()
      .setName(getName.apply(device))
      .setLaunchCompatibility(device.getLaunchCompatibility())
      .setKey(key)
      .setConnectionTime(map.get(key))
      .setAndroidDevice(device.getAndroidDevice())
      .setType(device.getType())
      .build();
  }

  @VisibleForTesting
  static final class Builder extends Device.Builder {
    @NotNull
    @VisibleForTesting
    Builder setName(@NotNull String name) {
      myName = name;
      return this;
    }

    @NotNull Builder setLaunchCompatibility(@NotNull LaunchCompatibility launchCompatibility) {
      myLaunchCompatibility = launchCompatibility;
      return this;
    }

    @NotNull
    @VisibleForTesting
    Builder setKey(@NotNull Key key) {
      myKey = key;
      return this;
    }

    @NotNull
    @VisibleForTesting
    Builder setConnectionTime(@NotNull Instant connectionTime) {
      myConnectionTime = connectionTime;
      return this;
    }

    @NotNull
    @VisibleForTesting
    Builder setAndroidDevice(@NotNull AndroidDevice androidDevice) {
      myAndroidDevice = androidDevice;
      return this;
    }

    @NotNull Builder setType(@NotNull Type type) {
      myType = type;
      return this;
    }

    @NotNull
    @Override
    PhysicalDevice build() {
      return new PhysicalDevice(this);
    }
  }

  private PhysicalDevice(@NotNull Builder builder) {
    super(builder);
  }

  @NotNull
  @Override
  Icon getIcon() {
    Icon deviceIcon;
    switch (getType()) {
      case TV:
        deviceIcon = ourTvIcon;
        break;
      case WEAR:
        deviceIcon = ourWearIcon;
        break;
      case PHONE:
        deviceIcon = ourPhoneIcon;
        break;
      default:
        throw new IllegalStateException("Unexpected device type: " + getType());
    }

    switch (getLaunchCompatibility().getState()) {
      case ERROR:
        return new LayeredIcon(deviceIcon, ERROR_DECORATOR);
      case WARNING:
        return new LayeredIcon(deviceIcon, AllIcons.General.WarningDecorator);
      case OK:
        return deviceIcon;
      default:
        throw new IllegalStateException("Unexpected device state: " + getLaunchCompatibility().getState());
    }
  }

  /**
   * @return true. Physical devices come and go as they are connected and disconnected; there are no instances of this class for
   * disconnected physical devices.
   */
  @Override
  boolean isConnected() {
    return true;
  }

  @Override
  @NotNull Collection<@NotNull Snapshot> getSnapshots() {
    return Collections.emptyList();
  }

  @Override
  @NotNull Target getDefaultTarget() {
    return new RunningDeviceTarget(getKey());
  }

  @Override
  @NotNull Collection<@NotNull Target> getTargets() {
    return Collections.singletonList(new RunningDeviceTarget(getKey()));
  }

  @NotNull
  @Override
  Future<AndroidVersion> getAndroidVersion() {
    IDevice device = getDdmlibDevice();
    assert device != null;

    return DeploymentApplicationService.getInstance().getVersion(device);
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof PhysicalDevice)) {
      return false;
    }

    Device device = (Device)object;

    return getName().equals(device.getName()) &&
           getLaunchCompatibility().equals(device.getLaunchCompatibility()) &&
           getKey().equals(device.getKey()) &&
           Objects.equals(getConnectionTime(), device.getConnectionTime()) &&
           getAndroidDevice().equals(device.getAndroidDevice());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getLaunchCompatibility(), getKey(), getConnectionTime(), getAndroidDevice());
  }
}
