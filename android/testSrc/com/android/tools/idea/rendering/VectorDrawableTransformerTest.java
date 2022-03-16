/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.rendering;

import static org.junit.Assert.assertEquals;

import com.intellij.openapi.util.text.StringUtil;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link VectorDrawableTransformer}.
 */
@RunWith(JUnit4.class)
public class VectorDrawableTransformerTest {
  private static final String ORIGINAL = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
      "    xmlns:aapt=\"http://schemas.android.com/aapt\"\n" +
      "    android:width=\"24dp\"\n" +
      "    android:height=\"24dp\"\n" +
      "    android:viewportHeight=\"108.0\"\n" +
      "    android:viewportWidth=\"108.0\"\n" +
      "    android:autoMirrored=\"true\">\n" +
      "  <!-- This is a shadow -->\n" +
      "  <path\n" +
      "      android:fillType=\"evenOdd\"\n" +
      "      android:pathData=\"M32,64C32,64 38.39,52.99 44.13,50.95C51.37,48.37 70.14,49.57 70.14,49.57L108.26,87.69L108,109.01L75.97,107.97L32,64Z\"\n" +
      "      android:strokeColor=\"#00000000\"\n" +
      "      android:strokeWidth=\"1\">\n" +
      "    <aapt:attr name=\"android:fillColor\">\n" +
      "      <gradient\n" +
      "          android:endX=\"78.58851490098495\"\n" +
      "          android:endY=\"90.91591973616056\"\n" +
      "          android:startX=\"48.765298831584175\"\n" +
      "          android:startY=\"61.092703666759796\"\n" +
      "          android:type=\"linear\">\n" +
      "        <item\n" +
      "            android:color=\"#26000000\"\n" +
      "            android:offset=\"0.0\" />\n" +
      "        <item\n" +
      "            android:color=\"#05000000\"\n" +
      "            android:offset=\"0.52042806\" />\n" +
      "        <item\n" +
      "            android:color=\"#00000000\"\n" +
      "            android:offset=\"1.0\" />\n" +
      "      </gradient>\n" +
      "    </aapt:attr>\n" +
      "  </path>\n" +
      "  <!-- This is an Android's head -->\n" +
      "  <path\n" +
      "      android:fillColor=\"#78C257\"\n" +
      "      android:fillType=\"nonZero\"\n" +
      "      android:pathData=\"M66.94,46.02L66.94,46.02C72.44,50.07 76,56.61 76,64L32,64C32,56.61 35.56,50.11 40.98,46.06L36.18,41.19C35.45,40.45 35.45,39.3 36.18,38.56C36.91,37.81 38.05,37.81 38.78,38.56L44.25,44.05C47.18,42.57 50.48,41.71 54,41.71C57.48,41.71 60.78,42.57 63.68,44.05L69.11,38.56C69.84,37.81 70.98,37.81 71.71,38.56C72.44,39.3 72.44,40.45 71.71,41.19L66.94,46.02ZM62.94,56.92C64.08,56.92 65,56.01 65,54.88C65,53.76 64.08,52.85 62.94,52.85C61.8,52.85 60.88,53.76 60.88,54.88C60.88,56.01 61.8,56.92 62.94,56.92ZM45.06,56.92C46.2,56.92 47.13,56.01 47.13,54.88C47.13,53.76 46.2,52.85 45.06,52.85C43.92,52.85 43,53.76 43,54.88C43,56.01 43.92,56.92 45.06,56.92Z\"\n" +
      "      android:strokeColor=\"#00000000\"\n" +
      "      android:strokeWidth=\"1\" />\n" +
      "</vector>\n";

  @Test
  public void testSizeAdjustment() throws Exception {
    String expected = "" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    xmlns:aapt=\"http://schemas.android.com/aapt\"\n" +
        "    android:width=\"108dp\"\n" +
        "    android:height=\"108dp\"\n" +
        "    android:viewportWidth=\"108\"\n" +
        "    android:viewportHeight=\"108\"\n" +
        "    android:autoMirrored=\"true\">\n" +
        "  <!-- This is a shadow -->\n" +
        "  <path\n" +
        "      android:fillType=\"evenOdd\"\n" +
        "      android:pathData=\"M32,64C32,64 38.39,52.99 44.13,50.95C51.37,48.37 70.14,49.57 70.14,49.57L108.26,87.69L108,109.01L75.97,107.97L32,64Z\"\n" +
        "      android:strokeColor=\"#00000000\"\n" +
        "      android:strokeWidth=\"1\">\n" +
        "    <aapt:attr name=\"android:fillColor\">\n" +
        "      <gradient\n" +
        "          android:endX=\"78.58851490098495\"\n" +
        "          android:endY=\"90.91591973616056\"\n" +
        "          android:startX=\"48.765298831584175\"\n" +
        "          android:startY=\"61.092703666759796\"\n" +
        "          android:type=\"linear\">\n" +
        "        <item\n" +
        "            android:color=\"#26000000\"\n" +
        "            android:offset=\"0.0\" />\n" +
        "        <item\n" +
        "            android:color=\"#05000000\"\n" +
        "            android:offset=\"0.52042806\" />\n" +
        "        <item\n" +
        "            android:color=\"#00000000\"\n" +
        "            android:offset=\"1.0\" />\n" +
        "      </gradient>\n" +
        "    </aapt:attr>\n" +
        "  </path>\n" +
        "  <!-- This is an Android's head -->\n" +
        "  <path\n" +
        "      android:fillColor=\"#78C257\"\n" +
        "      android:fillType=\"nonZero\"\n" +
        "      android:pathData=\"M66.94,46.02L66.94,46.02C72.44,50.07 76,56.61 76,64L32,64C32,56.61 35.56,50.11 40.98,46.06L36.18,41.19C35.45,40.45 35.45,39.3 36.18,38.56C36.91,37.81 38.05,37.81 38.78,38.56L44.25,44.05C47.18,42.57 50.48,41.71 54,41.71C57.48,41.71 60.78,42.57 63.68,44.05L69.11,38.56C69.84,37.81 70.98,37.81 71.71,38.56C72.44,39.3 72.44,40.45 71.71,41.19L66.94,46.02ZM62.94,56.92C64.08,56.92 65,56.01 65,54.88C65,53.76 64.08,52.85 62.94,52.85C61.8,52.85 60.88,53.76 60.88,54.88C60.88,56.01 61.8,56.92 62.94,56.92ZM45.06,56.92C46.2,56.92 47.13,56.01 47.13,54.88C47.13,53.76 46.2,52.85 45.06,52.85C43.92,52.85 43,53.76 43,54.88C43,56.01 43.92,56.92 45.06,56.92Z\"\n" +
        "      android:strokeColor=\"#00000000\"\n" +
        "      android:strokeWidth=\"1\" />\n" +
        "</vector>\n";
    String result = VectorDrawableTransformer.transform(ORIGINAL, new Dimension(108, 108), Gravity.CENTER, 1, null, null, null, 1);
    assertEquals(expected, result);
  }

  @Test
  public void testScaling() throws Exception {
    String expected = "" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    xmlns:aapt=\"http://schemas.android.com/aapt\"\n" +
        "    android:width=\"100dp\"\n" +
        "    android:height=\"100dp\"\n" +
        "    android:viewportWidth=\"100\"\n" +
        "    android:viewportHeight=\"100\"\n" +
        "    android:autoMirrored=\"true\">\n" +
        "  <group android:scaleX=\"2.3148148\"\n" +
        "      android:scaleY=\"2.3148148\"\n" +
        "      android:translateX=\"-75\"\n" +
        "      android:translateY=\"-75\">\n" +
        "    <!-- This is a shadow -->\n" +
        "    <path\n" +
        "        android:fillType=\"evenOdd\"\n" +
        "        android:pathData=\"M32,64C32,64 38.39,52.99 44.13,50.95C51.37,48.37 70.14,49.57 70.14,49.57L108.26,87.69L108,109.01L75.97,107.97L32,64Z\"\n" +
        "        android:strokeColor=\"#00000000\"\n" +
        "        android:strokeWidth=\"1\">\n" +
        "      <aapt:attr name=\"android:fillColor\">\n" +
        "        <gradient\n" +
        "            android:endX=\"78.58851490098495\"\n" +
        "            android:endY=\"90.91591973616056\"\n" +
        "            android:startX=\"48.765298831584175\"\n" +
        "            android:startY=\"61.092703666759796\"\n" +
        "            android:type=\"linear\">\n" +
        "          <item\n" +
        "              android:color=\"#26000000\"\n" +
        "              android:offset=\"0.0\" />\n" +
        "          <item\n" +
        "              android:color=\"#05000000\"\n" +
        "              android:offset=\"0.52042806\" />\n" +
        "          <item\n" +
        "              android:color=\"#00000000\"\n" +
        "              android:offset=\"1.0\" />\n" +
        "        </gradient>\n" +
        "      </aapt:attr>\n" +
        "    </path>\n" +
        "    <!-- This is an Android's head -->\n" +
        "    <path\n" +
        "        android:fillColor=\"#78C257\"\n" +
        "        android:fillType=\"nonZero\"\n" +
        "        android:pathData=\"M66.94,46.02L66.94,46.02C72.44,50.07 76,56.61 76,64L32,64C32,56.61 35.56,50.11 40.98,46.06L36.18,41.19C35.45,40.45 35.45,39.3 36.18,38.56C36.91,37.81 38.05,37.81 38.78,38.56L44.25,44.05C47.18,42.57 50.48,41.71 54,41.71C57.48,41.71 60.78,42.57 63.68,44.05L69.11,38.56C69.84,37.81 70.98,37.81 71.71,38.56C72.44,39.3 72.44,40.45 71.71,41.19L66.94,46.02ZM62.94,56.92C64.08,56.92 65,56.01 65,54.88C65,53.76 64.08,52.85 62.94,52.85C61.8,52.85 60.88,53.76 60.88,54.88C60.88,56.01 61.8,56.92 62.94,56.92ZM45.06,56.92C46.2,56.92 47.13,56.01 47.13,54.88C47.13,53.76 46.2,52.85 45.06,52.85C43.92,52.85 43,53.76 43,54.88C43,56.01 43.92,56.92 45.06,56.92Z\"\n" +
        "        android:strokeColor=\"#00000000\"\n" +
        "        android:strokeWidth=\"1\" />\n" +
        "  </group>\n" +
        "</vector>\n";
    String result = VectorDrawableTransformer.transform(ORIGINAL, new Dimension(100, 100), Gravity.CENTER, 2.5, null, null, null, 1);
    assertEquals(expected, result);
    // Check the same transformation but with Windows line separators.
    String original = StringUtil.replace(ORIGINAL, "\n", "\r\n");
    result = VectorDrawableTransformer.transform(original, new Dimension(100, 100), Gravity.CENTER, 2.5, null, null, null, 1);
    expected = StringUtil.replace(expected, "\n", "\r\n");
    assertEquals(expected, result);
  }

  @Test
  public void testScalingAndClipping() throws Exception {
    String expected = "" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    xmlns:aapt=\"http://schemas.android.com/aapt\"\n" +
        "    android:width=\"100dp\"\n" +
        "    android:height=\"100dp\"\n" +
        "    android:viewportWidth=\"100\"\n" +
        "    android:viewportHeight=\"100\"\n" +
        "    android:autoMirrored=\"true\">\n" +
        "  <group android:scaleX=\"1.5873016\"\n" +
        "      android:scaleY=\"1.5873016\"\n" +
        "      android:translateX=\"-44.285713\"\n" +
        "      android:translateY=\"-44.285713\">\n" +
        "    <!-- This is a shadow -->\n" +
        "    <path\n" +
        "        android:fillType=\"evenOdd\"\n" +
        "        android:pathData=\"M32,64C32,64 38.39,52.99 44.13,50.95C51.37,48.37 70.14,49.57 70.14,49.57L108.26,87.69L108,109.01L75.97,107.97L32,64Z\"\n" +
        "        android:strokeColor=\"#00000000\"\n" +
        "        android:strokeWidth=\"1\">\n" +
        "      <aapt:attr name=\"android:fillColor\">\n" +
        "        <gradient\n" +
        "            android:endX=\"78.58851490098495\"\n" +
        "            android:endY=\"90.91591973616056\"\n" +
        "            android:startX=\"48.765298831584175\"\n" +
        "            android:startY=\"61.092703666759796\"\n" +
        "            android:type=\"linear\">\n" +
        "          <item\n" +
        "              android:color=\"#26000000\"\n" +
        "              android:offset=\"0.0\" />\n" +
        "          <item\n" +
        "              android:color=\"#05000000\"\n" +
        "              android:offset=\"0.52042806\" />\n" +
        "          <item\n" +
        "              android:color=\"#00000000\"\n" +
        "              android:offset=\"1.0\" />\n" +
        "        </gradient>\n" +
        "      </aapt:attr>\n" +
        "    </path>\n" +
        "    <!-- This is an Android's head -->\n" +
        "    <path\n" +
        "        android:fillColor=\"#78C257\"\n" +
        "        android:fillType=\"nonZero\"\n" +
        "        android:pathData=\"M66.94,46.02L66.94,46.02C72.44,50.07 76,56.61 76,64L32,64C32,56.61 35.56,50.11 40.98,46.06L36.18,41.19C35.45,40.45 35.45,39.3 36.18,38.56C36.91,37.81 38.05,37.81 38.78,38.56L44.25,44.05C47.18,42.57 50.48,41.71 54,41.71C57.48,41.71 60.78,42.57 63.68,44.05L69.11,38.56C69.84,37.81 70.98,37.81 71.71,38.56C72.44,39.3 72.44,40.45 71.71,41.19L66.94,46.02ZM62.94,56.92C64.08,56.92 65,56.01 65,54.88C65,53.76 64.08,52.85 62.94,52.85C61.8,52.85 60.88,53.76 60.88,54.88C60.88,56.01 61.8,56.92 62.94,56.92ZM45.06,56.92C46.2,56.92 47.13,56.01 47.13,54.88C47.13,53.76 46.2,52.85 45.06,52.85C43.92,52.85 43,53.76 43,54.88C43,56.01 43.92,56.92 45.06,56.92Z\"\n" +
        "        android:strokeColor=\"#00000000\"\n" +
        "        android:strokeWidth=\"1\" />\n" +
        "  </group>\n" +
        "</vector>\n";
    String result = VectorDrawableTransformer.transform(ORIGINAL, new Dimension(100, 100), Gravity.CENTER, 1.2,
                                                        new Rectangle2D.Double(0.2, 0.3, 0.7, 0.5), null, null, 1);
    assertEquals(expected, result);
  }

  @Test
  public void testScalingWithGroups() throws Exception {
    String original = "" +
                      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:width=\"94dp\"\n" +
                      "    android:height=\"337.5dp\"\n" +
                      "    android:viewportWidth=\"94\"\n" +
                      "    android:viewportHeight=\"337.5\">\n" +
                      "  <group android:translateY=\"133.59375\">\n" +
                      "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "        android:fillColor=\"#000000\"/>\n" +
                      "  </group>\n" +
                      "  <group android:translateY=\"302.34375\">\n" +
                      "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "        android:fillColor=\"#000000\"/>\n" +
                      "  </group>\n" +
                      "</vector>\n";
    String expected = "" +
                      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:width=\"100dp\"\n" +
                      "    android:height=\"100dp\"\n" +
                      "    android:viewportWidth=\"100\"\n" +
                      "    android:viewportHeight=\"100\">\n" +
                      "  <group android:scaleX=\"0.7407407\"\n" +
                      "      android:scaleY=\"0.7407407\"\n" +
                      "      android:translateX=\"15.185185\"\n" +
                      "      android:translateY=\"-75\">\n" +
                      "    <group android:translateY=\"133.59375\">\n" +
                      "      <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "          android:fillColor=\"#000000\"/>\n" +
                      "    </group>\n" +
                      "    <group android:translateY=\"302.34375\">\n" +
                      "      <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "          android:fillColor=\"#000000\"/>\n" +
                      "    </group>\n" +
                      "  </group>\n" +
                      "</vector>\n";
    String result = VectorDrawableTransformer.transform(original, new Dimension(100, 100), Gravity.CENTER, 2.5, null, null, null, 1);
    assertEquals(expected, result);
  }

  @Test
  public void testShift() throws Exception {
    String original = "" +
                      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:width=\"94dp\"\n" +
                      "    android:height=\"337.5dp\"\n" +
                      "    android:viewportWidth=\"94\"\n" +
                      "    android:viewportHeight=\"337.5\">\n" +
                      "  <group android:translateY=\"133.59375\">\n" +
                      "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "        android:fillColor=\"#000000\"/>\n" +
                      "  </group>\n" +
                      "  <group android:translateY=\"302.34375\">\n" +
                      "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "        android:fillColor=\"#000000\"/>\n" +
                      "  </group>\n" +
                      "</vector>\n";
    String expected = "" +
                      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:width=\"100dp\"\n" +
                      "    android:height=\"100dp\"\n" +
                      "    android:viewportWidth=\"100\"\n" +
                      "    android:viewportHeight=\"100\">\n" +
                      "  <group android:scaleX=\"0.7407407\"\n" +
                      "      android:scaleY=\"0.7407407\"\n" +
                      "      android:translateX=\"25.185184\"\n" +
                      "      android:translateY=\"-95\">\n" +
                      "    <group android:translateY=\"133.59375\">\n" +
                      "      <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "          android:fillColor=\"#000000\"/>\n" +
                      "    </group>\n" +
                      "    <group android:translateY=\"302.34375\">\n" +
                      "      <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "          android:fillColor=\"#000000\"/>\n" +
                      "    </group>\n" +
                      "  </group>\n" +
                      "</vector>\n";
    String result = VectorDrawableTransformer.transform(original, new Dimension(100, 100), Gravity.CENTER, 2.5, null, new Point2D.Double(0.1, -0.2), null, 1);
    assertEquals(expected, result);
  }

  @Test
  public void testUnscalableDrawable() throws Exception {
    String original = "" +
        "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    android:shape=\"rectangle\">\n" +
        "  <gradient\n" +
        "      android:type=\"linear\"\n" +
        "      android:angle=\"90\"\n" +
        "      android:centerX=\"63%\"\n" +
        "      android:startColor=\"#FF36C3FF\"\n" +
        "      android:centerColor=\"#FF5BFFD9\"\n" +
        "      android:endColor=\"#FF5BFFD9\"/>\n" +
        "</shape>\n";
    String result = VectorDrawableTransformer.transform(original, new Dimension(100, 100), Gravity.CENTER, 1.2,
                                                        new Rectangle2D.Double(0.2, 0.3, 0.7, 0.5), null, null, 1);
    assertEquals(original, result);
  }

  @Test
  public void testMerge() throws Exception {
    String drawable1 = "" +
                      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                      "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                      "    android:width=\"94dp\"\n" +
                      "    android:height=\"337.5dp\"\n" +
                      "    android:viewportWidth=\"94\"\n" +
                      "    android:viewportHeight=\"337.5\">\n" +
                      "  <group android:translateY=\"133.59375\">\n" +
                      "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                      "        android:fillColor=\"#000000\"/>\n" +
                      "  </group>\n" +
                      "</vector>\n";
    String drawable2 = "" +
                       "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                       "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                       "    android:width=\"94dp\"\n" +
                       "    android:height=\"337.5dp\"\n" +
                       "    android:viewportWidth=\"94\"\n" +
                       "    android:viewportHeight=\"337.5\">\n" +
                       "  <group android:translateY=\"302.34375\">\n" +
                       "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                       "        android:fillColor=\"#000000\"/>\n" +
                       "  </group>\n" +
                       "</vector>\n";
    String merged = "" +
                       "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                       "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                       "    android:width=\"94dp\"\n" +
                       "    android:height=\"337.5dp\"\n" +
                       "    android:viewportWidth=\"94\"\n" +
                       "    android:viewportHeight=\"337.5\">\n" +
                       "  <group android:translateY=\"133.59375\">\n" +
                       "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                       "        android:fillColor=\"#000000\"/>\n" +
                       "  </group>\n" +
                       "\n" +
                       "  <group android:translateY=\"302.34375\">\n" +
                       "    <path android:pathData=\"M68.41406,-26.71875L25.523438,-26.71875L15.890625,0L1.96875,0L41.0625,-102.375L52.875,-102.375L92.03906,0L78.1875,0ZM29.601562,-37.828125L64.40625,-37.828125L46.96875,-85.71094Z\"\n" +
                       "        android:fillColor=\"#000000\"/>\n" +
                       "  </group>\n" +
                       "</vector>\n";
    String result = VectorDrawableTransformer.merge(drawable1, drawable2);
    assertEquals(merged, result);
  }
}
