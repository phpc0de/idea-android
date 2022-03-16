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
package com.android.tools.idea.run.deployment;

import static org.junit.Assert.assertEquals;

import com.android.tools.idea.run.AndroidDevice;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public final class SelectMultipleDevicesDialogTableModelRowTest {
  private static final Key KEY = new VirtualDevicePath("/home/user/.android/avd/Pixel_4_API_30.avd");
  private static final Target TARGET = new QuickBootTarget(KEY);

  @Test
  public void getBootOptionDeviceIsConnected() {
    // Arrange
    Device device = new VirtualDevice.Builder()
      .setName("Pixel 4 API 30")
      .setKey(KEY)
      .setConnectionTime(Instant.parse("2018-11-28T01:15:27Z"))
      .setAndroidDevice(Mockito.mock(AndroidDevice.class))
      .build();

    SelectMultipleDevicesDialogTableModelRow row = new SelectMultipleDevicesDialogTableModelRow(device, () -> true, TARGET);

    // Act
    Object option = row.getBootOption();

    // Assert
    assertEquals("", option);
  }

  @Test
  public void getBootOptionSnapshotsIsEmpty() {
    // Arrange
    Device device = new VirtualDevice.Builder()
      .setName("Pixel 4 API 30")
      .setKey(KEY)
      .setAndroidDevice(Mockito.mock(AndroidDevice.class))
      .build();

    SelectMultipleDevicesDialogTableModelRow row = new SelectMultipleDevicesDialogTableModelRow(device, () -> true, TARGET);

    // Act
    Object option = row.getBootOption();

    // Assert
    assertEquals("", option);
  }
}
