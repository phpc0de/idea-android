// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.android.tools.idea.logcat;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.StudioIcons;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * An entry in the "Colors and Fonts" settings section for Android Logcat settings.
 */
public final class AndroidLogcatColorPage implements ColorSettingsPage {
  private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS = new HashMap<String, TextAttributesKey>();
  private static final String DEMO_TEXT = "Logcat:\n" +
                                          "<verbose>02-02 18:52:57.132: VERBOSE/ProtocolEngine(24): DownloadRate 104166 bytes per sec. Downloaded Bytes 5643/34714</verbose>\n" +
                                          "<debug>08-03 13:31:16.196: DEBUG/dalvikvm(2227): HeapWorker thread shutting down</debug>\n" +
                                          "<info>08-03 13:31:16.756: INFO/dalvikvm(2234): Debugger is active</info>\n" +
                                          "<warning>08-03 16:26:45.965: WARN/ActivityManager(564): Launch timeout has expired, giving up wake lock!</warning>\n" +
                                          "<error>08-04 16:19:11.166: ERROR/AndroidRuntime(4687): Uncaught handler: thread main exiting due to uncaught exception</error>\n" +
                                          "<assert>08-04 16:24:11.166: ASSERT/Assertion(4687): Expected true but was false</assert>";

  static {
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("verbose", AndroidLogcatConstants.VERBOSE_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("debug", AndroidLogcatConstants.DEBUG_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("info", AndroidLogcatConstants.INFO_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("warning", AndroidLogcatConstants.WARNING_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("error", AndroidLogcatConstants.ERROR_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("assert", AndroidLogcatConstants.ASSERT_OUTPUT_KEY);
  }

  private static final AttributesDescriptor[] ATTRIBUTES_DESCRIPTORS =
    new AttributesDescriptor[]{new AttributesDescriptor(AndroidBundle.message("verbose.level.title"), AndroidLogcatConstants.VERBOSE_OUTPUT_KEY),
      new AttributesDescriptor(AndroidBundle.message("info.level.title"), AndroidLogcatConstants.INFO_OUTPUT_KEY),
      new AttributesDescriptor(AndroidBundle.message("debug.level.title"), AndroidLogcatConstants.DEBUG_OUTPUT_KEY),
      new AttributesDescriptor(AndroidBundle.message("warning.level.title"), AndroidLogcatConstants.WARNING_OUTPUT_KEY),
      new AttributesDescriptor(AndroidBundle.message("error.level.title"), AndroidLogcatConstants.ERROR_OUTPUT_KEY),
      new AttributesDescriptor(AndroidBundle.message("assert.level.title"), AndroidLogcatConstants.ASSERT_OUTPUT_KEY)};

  @Override
  @NotNull
  public String getDisplayName() {
    return AndroidBundle.message("android.logcat.color.page.name");
  }

  @Override
  public Icon getIcon() {
    return StudioIcons.Common.ANDROID_HEAD;
  }

  @Override
  @NotNull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRIBUTES_DESCRIPTORS;
  }

  @Override
  @NotNull
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public SyntaxHighlighter getHighlighter() {
    return new PlainSyntaxHighlighter();
  }

  @Override
  @NotNull
  public String getDemoText() {
    return DEMO_TEXT;
  }

  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL_HIGHLIGHT_DESCRIPTORS;
  }
}
