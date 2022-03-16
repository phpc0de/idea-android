// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.testFramework.LightPlatformTestCase;

public class PluginDescriptorTest extends LightPlatformTestCase {
  public void testPluginDescription() {
    IdeaPluginDescriptor androidPlugin = PluginManager.getInstance().findEnabledPlugin(PluginId.getId("org.jetbrains.android"));

    assertNotNull(androidPlugin);
    assertTrue(androidPlugin.getDescription().startsWith("Supports the development of"));
  }

  public void testPluginBundle() {
    IdeaPluginDescriptor androidPlugin = PluginManager.getInstance().findEnabledPlugin(PluginId.getId("org.jetbrains.android"));

    assertNotNull(androidPlugin);
    assertEquals("messages.AndroidBundle", androidPlugin.getResourceBundleBaseName());
  }
}
