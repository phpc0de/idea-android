/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.testing;

import com.android.tools.idea.gradle.project.sync.SdkSync;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IdeComponents}.
 */
public class IdeComponentsTest extends PlatformTestCase {

  public void testReplaceApplicationService() {
    SdkSync originalSdkSync = SdkSync.getInstance();
    try (TempDisposable scope = new TempDisposable()){
      SdkSync mockSdkSync = mock(SdkSync.class);
      new IdeComponents(myProject, scope).replaceApplicationService(SdkSync.class, mockSdkSync);
      assertSame(mockSdkSync, SdkSync.getInstance());
    }
    finally {
      assertSame(originalSdkSync, SdkSync.getInstance());
    }
  }

  public void testReplaceProjectService() {
    GradleSettings originalSettings = GradleSettings.getInstance(getProject());
    try (TempDisposable scope = new TempDisposable()){
      GradleSettings mockSettings = mock(GradleSettings.class);
      new IdeComponents(getProject(), scope).replaceProjectService(GradleSettings.class, mockSettings);
      assertSame(mockSettings, GradleSettings.getInstance(getProject()));
    } finally {
      assertSame(originalSettings, GradleSettings.getInstance(getProject()));
    }
  }
}
