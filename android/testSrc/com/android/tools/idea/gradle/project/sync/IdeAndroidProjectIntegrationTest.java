/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync;

import com.android.tools.idea.gradle.model.impl.IdeAndroidProjectImpl;
import com.android.tools.idea.testing.TestProjectPaths;

/**
 * Tests for {@link IdeAndroidProjectImpl}.
 */
public class IdeAndroidProjectIntegrationTest extends IdeAndroidProjectIntegrationTestCase {

  public void testLevel2DependenciesWithHeadPlugin() throws Exception {
    loadSimpleApplication();
    verifyIdeLevel2DependenciesPopulated();
  }

  public void testLocalAarsAsModulesWithHeadPlugin() throws Exception {
    loadProject(TestProjectPaths.LOCAL_AARS_AS_MODULES);
    verifyAarModuleShowsAsAndroidLibrary("artifacts:library-debug:unspecified@jar");
  }
}
