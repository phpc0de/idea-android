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
package com.android.tools.idea.run;

import com.android.ddmlib.IDevice;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.devices.Abi;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.tools.idea.ddms.DeviceNameRendererEx;
import com.android.tools.idea.ddms.DevicePropertyUtil;
import com.android.tools.idea.run.util.LaunchUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ConnectedAndroidDevice implements AndroidDevice {
  private static final ExtensionPointName<DeviceNameRendererEx> EP_NAME = ExtensionPointName.create("com.android.run.deviceNameRenderer");

  @NotNull private final IDevice myDevice;
  @Nullable private final String myAvdName;
  @Nullable private final DeviceNameRendererEx myDeviceNameRenderer;
  private volatile String myDeviceManufacturer;
  private volatile String myDeviceModel;

  public ConnectedAndroidDevice(@NotNull IDevice device, @Nullable List<AvdInfo> avds) {
    myDevice = device;

    AvdInfo avd = getAvdInfo(device, avds);
    myAvdName = avd == null ? null : avd.getDisplayName();
    myDeviceNameRenderer = getRendererExtension(device);
  }

  @Nullable
  private static AvdInfo getAvdInfo(@NotNull IDevice device, @Nullable List<AvdInfo> avds) {
    if (avds != null && device.isEmulator()) {
      for (AvdInfo avd : avds) {
        if (avd.getName().equals(device.getAvdName())) {
          return avd;
        }
      }
    }

    return null;
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public boolean isVirtual() {
    return myDevice.isEmulator();
  }

  @NotNull
  @Override
  public AndroidVersion getVersion() {
    return myDevice.getVersion();
  }

  @Override
  public int getDensity() {
    return myDevice.getDensity();
  }

  @NotNull
  @Override
  public List<Abi> getAbis() {
    List<String> abis = myDevice.getAbis();
    ImmutableList.Builder<Abi> builder = ImmutableList.builder();

    for (String abi : abis) {
      Abi a = Abi.getEnum(abi);
      if (a != null) {
        builder.add(a);
      }
    }

    return builder.build();
  }

  @NotNull
  @Override
  public String getSerial() {
    if (myDevice.isEmulator()) {
      String avdName = myDevice.getAvdName();
      if (avdName != null) {
        return avdName;
      }
    }

    return myDevice.getSerialNumber();
  }

  @Override
  public boolean supportsFeature(@NotNull IDevice.HardwareFeature feature) {
    return myDevice.supportsFeature(feature);
  }

  @NotNull
  @Override
  public String getName() {
    if (myDeviceNameRenderer != null) {
      return myDeviceNameRenderer.getName(myDevice);
    }

    if (isVirtual()) {
      if (myAvdName != null) {
        return myAvdName;
      }
      else {
        // if the avd name is not available, then include the serial number in order to differentiate
        // between multiple emulators
        return getDeviceName() + " [" + getSerial() + "]";
      }
    }
    return getDeviceName();
  }

  @NotNull
  private String getDeviceName() {
    StringBuilder name = new StringBuilder(20);
    name.append(getDeviceManufacturer());
    if (name.length() > 0) {
      name.append(' ');
    }
    name.append(getDeviceModel());
    return name.toString();
  }

  @NotNull
  private String getDeviceManufacturer() {
    if (myDeviceManufacturer == null) {
      assert isNotDispatchThread();
      myDeviceManufacturer = DevicePropertyUtil.getManufacturer(myDevice, "");
    }
    return myDeviceManufacturer;
  }

  @NotNull
  private String getDeviceModel() {
    if (myDeviceModel == null) {
      assert isNotDispatchThread();
      myDeviceModel = DevicePropertyUtil.getModel(myDevice, "");
    }
    return myDeviceModel;
  }

  @NotNull
  @Override
  public ListenableFuture<IDevice> launch(@NotNull Project project) {
    return getLaunchedDevice();
  }

  @NotNull
  @Override
  public ListenableFuture<IDevice> getLaunchedDevice() {
    return Futures.immediateFuture(myDevice);
  }

  @Nullable
  private static DeviceNameRendererEx getRendererExtension(@NotNull IDevice device) {
    Application application = ApplicationManager.getApplication();
    if (application == null || application.isUnitTestMode()) {
      return null;
    }
    for (DeviceNameRendererEx extensionRenderer : EP_NAME.getExtensions()) {
      if (extensionRenderer.isApplicable(device)) {
        return extensionRenderer;
      }
    }
    return null;
  }

  @Override
  @NotNull
  public LaunchCompatibility canRun(@NotNull AndroidVersion minSdkVersion,
                                    @NotNull IAndroidTarget projectTarget,
                                    @NotNull AndroidFacet facet,
                                    Function<AndroidFacet, EnumSet<IDevice.HardwareFeature>> getRequiredHardwareFeatures,
                                    @NotNull Set<String> supportedAbis) {
    return LaunchCompatibility.canRunOnDevice(minSdkVersion, projectTarget, facet, getRequiredHardwareFeatures, supportedAbis, this);
  }

  private boolean isNotDispatchThread() {
    Application application = ApplicationManager.getApplication();
    return application == null || !application.isDispatchThread();
  }

  @Override
  public boolean isDebuggable() {
    return LaunchUtils.isDebuggableDevice(myDevice);
  }
}
