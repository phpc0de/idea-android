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
package com.android.tools.idea.avdmanager;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.repository.generated.common.v1.IdDisplayType;
import icons.StudioIcons;
import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.android.AndroidTestCase;

public class AvdDisplayListTest extends AndroidTestCase {
  private AvdInfo myAvdInfo;
  private final Map<String, String> myPropertiesMap = new HashMap<>();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myAvdInfo = new AvdInfo("name", new File("ini"), "folder", null, myPropertiesMap);
  }

  public void testGetResolution() throws Exception {
    assertEquals("Unknown Resolution", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 5");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("1080 × 1920: xxhdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 10");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("2560 × 1600: xhdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus S");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("480 × 800: hdpi", AvdDisplayList.getResolution(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 6");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertEquals("1440 × 2560: 560dpi", AvdDisplayList.getResolution(myAvdInfo));
  }

  public void testGetScreenSize() {
    assertNull(AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 5");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(1080, 1920, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 10");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(2560, 1600, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus S");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(480, 800, AvdDisplayList.getScreenSize(myAvdInfo));

    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_NAME, "Nexus 6");
    myPropertiesMap.put(AvdManager.AVD_INI_DEVICE_MANUFACTURER, "Google");
    assertDimension(1440, 2560, AvdDisplayList.getScreenSize(myAvdInfo));
  }

  public void testGetDeviceClassIconPair() {
    assertEquals(StudioIcons.Avd.DEVICE_MOBILE_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());

    myPropertiesMap.put(AvdManager.AVD_INI_TAG_ID, "android-tv");
    assertEquals(StudioIcons.Avd.DEVICE_TV_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());

    myPropertiesMap.put(AvdManager.AVD_INI_TAG_ID, "android-wear");
    assertEquals(StudioIcons.Avd.DEVICE_WEAR_LARGE, AvdDisplayList.getDeviceClassIconPair(myAvdInfo).getBaseIcon());
  }

  public void testTargetString() {
    AndroidVersion version =  new AndroidVersion(24, null);
    IdDisplayType displayType = new IdDisplayType();
    displayType.setId("default");
    displayType.setDisplay("");
    String targetString = AvdDisplayList.targetString(version, displayType);
    assertEquals("Android 7.0", targetString);

    version =  new AndroidVersion(26, null);
    displayType.setId("google_apis");
    displayType.setDisplay("Google APIs");
    targetString = AvdDisplayList.targetString(version, displayType);
    assertEquals("Android 8.0 (Google APIs)", targetString);

    version =  new AndroidVersion(27, null);
    displayType.setId("google_apis_playstore");
    displayType.setDisplay("Google Play");
    targetString = AvdDisplayList.targetString(version, displayType);
    assertEquals("Android 8.1 (Google Play)", targetString);

    version = new AndroidVersion(98, null);
    targetString = AvdDisplayList.targetString(version, displayType);
    assertEquals("Android API 98 (Google Play)", targetString);

    version = new AndroidVersion(98, "some preview");
    displayType.setId("default");
    displayType.setDisplay("");
    targetString = AvdDisplayList.targetString(version, displayType);
    assertEquals("Android API 99", targetString);
  }

  private static void assertDimension(double width, double height, Dimension dimension) {
    assertEquals(width, dimension.getWidth());
    assertEquals(height, dimension.getHeight());
  }
}