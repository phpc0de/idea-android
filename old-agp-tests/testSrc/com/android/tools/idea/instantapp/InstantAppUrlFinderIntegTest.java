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
package com.android.tools.idea.instantapp;

import static com.android.tools.idea.testing.TestProjectPaths.INSTANT_APP_RESOURCE_HOST;

import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.intellij.openapi.module.Module;
import java.util.Collection;

public class InstantAppUrlFinderIntegTest extends AndroidGradleTestCase {

  public void testHostIsResolved() throws Exception {
    // Use a plugin with instant app support
    loadProject(INSTANT_APP_RESOURCE_HOST, null, null, "3.5.0");
    Module featureModule = getModule("feature");
    Collection<String> urls = new InstantAppUrlFinder(featureModule).getAllUrls();
    assertSize(1, urls);
    assertContainsElements(urls, "https://android.example.com/example");
  }
}
