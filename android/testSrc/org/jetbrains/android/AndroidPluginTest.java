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
package org.jetbrains.android;

import com.android.tools.idea.IdeInfo;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.extensions.PluginId;

public class AndroidPluginTest extends AndroidTestCase {
  public void testPluginIsEssential() {
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.android"));
    assertNotNull(plugin);

    assertEquals(IdeInfo.getInstance().isAndroidStudio(),
                 ApplicationInfoImpl.getShadowInstance().isEssentialPlugin("org.jetbrains.android"));
  }
}
