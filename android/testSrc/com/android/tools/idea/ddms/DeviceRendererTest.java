/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.ddms;

import static com.android.ddmlib.IDevice.DeviceState;
import static com.android.ddmlib.IDevice.PROP_BUILD_API_LEVEL;
import static com.android.ddmlib.IDevice.PROP_BUILD_VERSION;
import static com.android.ddmlib.IDevice.PROP_DEVICE_MANUFACTURER;
import static com.android.ddmlib.IDevice.PROP_DEVICE_MODEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.ddmlib.IDevice;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleColoredText;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public final class DeviceRendererTest {
  private static String manufacturer = "google";
  private static String model = "nexus 4";
  private static String buildVersion = "4.2";
  private static String apiLevel = "17";

  private static IDevice createDevice(boolean isEmulator,
                                      String avdName,
                                      String manufacturer,
                                      String model,
                                      String buildVersion,
                                      String apiLevel,
                                      String serial,
                                      DeviceState state) {
    IDevice d = EasyMock.createMock(IDevice.class);
    EasyMock.expect(d.isEmulator()).andStubReturn(isEmulator);
    if (isEmulator) {
      EasyMock.expect(d.getAvdName()).andStubReturn(avdName);
    }
    else {
      EasyMock.expect(d.getProperty(PROP_DEVICE_MANUFACTURER)).andStubReturn(manufacturer);
      EasyMock.expect(d.getProperty(PROP_DEVICE_MODEL)).andStubReturn(model);
    }
    EasyMock.expect(d.getProperty(PROP_BUILD_VERSION)).andStubReturn(buildVersion);
    EasyMock.expect(d.getProperty(PROP_BUILD_API_LEVEL)).andStubReturn(apiLevel);
    EasyMock.expect(d.getSerialNumber()).andStubReturn(serial);
    EasyMock.expect(d.getState()).andStubReturn(state);
    EasyMock.expect(d.getName()).andStubReturn(manufacturer + model);
    EasyMock.replay(d);
    return d;
  }

  @Test
  public void deviceNameRendering1() {
    String serial = "123";
    IDevice d = createDevice(false, null, manufacturer, model, buildVersion, apiLevel, serial, DeviceState.ONLINE);
    DeviceNameProperties deviceNameProperties = new DeviceNameProperties(model, manufacturer, buildVersion, apiLevel);
    SimpleColoredText target = new SimpleColoredText();
    DeviceRenderer.renderDeviceName(d, deviceNameProperties, target, false);

    String name = target.toString();

    assertEquals(String.format("%s %s Android %s, API %s",
                               StringUtil.capitalizeWords(manufacturer, false),
                               StringUtil.capitalizeWords(model, false),
                               buildVersion,
                               apiLevel),
                 name);
    // status should be shown only if !online
    assertFalse(StringUtil.containsIgnoreCase(name, DeviceState.ONLINE.toString()));
    // serial should be shown only if !online
    assertFalse(StringUtil.containsIgnoreCase(name, serial));
  }

  @Test
  public void deviceNameRendering2() {
    String serial = "123";
    IDevice d = createDevice(true, "Avdname", manufacturer, model, buildVersion, apiLevel, serial, DeviceState.BOOTLOADER);
    DeviceNameProperties deviceNameProperties = new DeviceNameProperties(model, manufacturer, buildVersion, apiLevel);
    SimpleColoredText target = new SimpleColoredText();
    DeviceRenderer.renderDeviceName(d, deviceNameProperties, target, false);

    String name = target.toString();
    assertFalse(StringUtil.containsIgnoreCase(name, "Nexus 4"));
    assertTrue(StringUtil.containsIgnoreCase(name, "Avdname"));
    assertTrue(StringUtil.containsIgnoreCase(name, DeviceState.BOOTLOADER.toString()));
    assertTrue(StringUtil.containsIgnoreCase(name, serial));
  }

  @Test
  public void deviceNameRenderingSerial() {
    String serial = "123";
    IDevice d = createDevice(false, null, manufacturer, model, buildVersion, apiLevel, serial, DeviceState.ONLINE);
    DeviceNameProperties deviceNameProperties = new DeviceNameProperties(model, manufacturer, buildVersion, apiLevel);
    SimpleColoredText target = new SimpleColoredText();
    DeviceRenderer.renderDeviceName(d, deviceNameProperties, target, true);

    String name = target.toString();

    // includes serial since we called the render method with show serial true
    assertEquals("Google Nexus 4 123 Android 4.2, API 17", name);
  }

  @Test
  public void nullAvdNameForEmulator() {
    String serial = "emulator-5554";
    IDevice d = createDevice(true, null, manufacturer, model, buildVersion, apiLevel, serial, DeviceState.ONLINE);
    DeviceNameProperties deviceNameProperties = new DeviceNameProperties(model, manufacturer, buildVersion, apiLevel);
    SimpleColoredText target = new SimpleColoredText();
    DeviceRenderer.renderDeviceName(d, deviceNameProperties, target, false);

    String name = target.toString();
    assertEquals("Emulator emulator-5554 Android 4.2, API 17", name);
  }

  @Test
  public void showSerialFalseForEmulator() {
    List<IDevice> devices = new ArrayList<>();

    IDevice d1 = createDevice(true, null, manufacturer, model, buildVersion, apiLevel, "123", DeviceState.ONLINE);
    devices.add(d1);
    devices.add(createDevice(true, null, manufacturer, model, buildVersion, apiLevel, "1234", DeviceState.ONLINE));

    assertFalse(DeviceRenderer.shouldShowSerialNumbers(devices, DeviceRendererTest::newDeviceNameProperties));
  }

  @Test
  public void showSerialTrueForDuplicate() {
    List<IDevice> devices = new ArrayList<>();

    IDevice d1 = createDevice(false, null, manufacturer, model, buildVersion, apiLevel, "123", DeviceState.ONLINE);
    devices.add(d1);
    devices.add(createDevice(false, null, manufacturer, model, buildVersion, apiLevel, "1234", DeviceState.ONLINE));

    assertTrue(DeviceRenderer.shouldShowSerialNumbers(devices, DeviceRendererTest::newDeviceNameProperties));
  }

  @NotNull
  private static DeviceNameProperties newDeviceNameProperties(@NotNull IDevice device) {
    return new DeviceNameProperties(DevicePropertyUtil.getModel(device, ""), DevicePropertyUtil.getManufacturer(device, ""), null, null);
  }
}
