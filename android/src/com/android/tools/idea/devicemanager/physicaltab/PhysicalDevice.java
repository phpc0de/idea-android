/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.devicemanager.physicaltab;

import com.android.tools.idea.devicemanager.Device;
import icons.StudioIcons;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PhysicalDevice extends Device implements Comparable<@NotNull PhysicalDevice> {
  private static final @NotNull Comparator<@NotNull PhysicalDevice> COMPARATOR =
    Comparator.<PhysicalDevice, Boolean>comparing(Device::isOnline, Comparator.reverseOrder())
      .thenComparing(PhysicalDevice::getLastOnlineTime, Comparator.nullsLast(Comparator.reverseOrder()));

  private final @NotNull String mySerialNumber;
  private final @Nullable Instant myLastOnlineTime;

  public static final class Builder extends Device.Builder {
    private @Nullable String mySerialNumber;
    private @Nullable Instant myLastOnlineTime;

    // TODO Initialize myName and myTarget properly
    public Builder() {
      myName = "Physical Device";
      myTarget = "Target";
    }

    public @NotNull Builder setSerialNumber(@NotNull String serialNumber) {
      mySerialNumber = serialNumber;
      return this;
    }

    @NotNull Builder setLastOnlineTime(@Nullable Instant lastOnlineTime) {
      myLastOnlineTime = lastOnlineTime;
      return this;
    }

    @NotNull Builder setName(@NotNull String name) {
      myName = name;
      return this;
    }

    public @NotNull Builder setOnline(@SuppressWarnings("SameParameterValue") boolean online) {
      myOnline = online;
      return this;
    }

    @NotNull Builder setTarget(@NotNull String target) {
      myTarget = target;
      return this;
    }

    @Override
    public @NotNull PhysicalDevice build() {
      return new PhysicalDevice(this);
    }
  }

  private PhysicalDevice(@NotNull Builder builder) {
    super(builder);

    assert builder.mySerialNumber != null;
    mySerialNumber = builder.mySerialNumber;

    myLastOnlineTime = builder.myLastOnlineTime;
  }

  @NotNull String getSerialNumber() {
    return mySerialNumber;
  }

  @Nullable Instant getLastOnlineTime() {
    return myLastOnlineTime;
  }

  @NotNull String toDebugString() {
    String separator = System.lineSeparator();

    return "serialNumber = " + mySerialNumber + separator
           + "lastOnlineTime = " + myLastOnlineTime + separator
           + "name = " + myName + separator
           + "online = " + myOnline + separator
           + "target = " + myTarget + separator;
  }

  @Override
  protected @NotNull Icon getIcon() {
    return StudioIcons.DeviceExplorer.PHYSICAL_DEVICE_PHONE;
  }

  @Override
  public int hashCode() {
    int hashCode = mySerialNumber.hashCode();

    hashCode = 31 * hashCode + Objects.hashCode(myLastOnlineTime);
    hashCode = 31 * hashCode + myName.hashCode();
    hashCode = 31 * hashCode + Boolean.hashCode(myOnline);
    hashCode = 31 * hashCode + myTarget.hashCode();

    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof PhysicalDevice)) {
      return false;
    }

    PhysicalDevice device = (PhysicalDevice)object;

    return mySerialNumber.equals(device.mySerialNumber) &&
           Objects.equals(myLastOnlineTime, device.myLastOnlineTime) &&
           myName.equals(device.myName) &&
           myOnline == device.myOnline &&
           myTarget.equals(device.myTarget);
  }

  @Override
  public int compareTo(@NotNull PhysicalDevice device) {
    return COMPARATOR.compare(this, device);
  }
}
