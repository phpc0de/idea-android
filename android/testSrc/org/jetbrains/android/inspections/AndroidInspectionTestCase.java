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
package org.jetbrains.android.inspections;

import com.android.SdkConstants;
import com.android.tools.idea.model.MergedManifestManager;
import com.android.tools.idea.model.MergedManifestModificationListener;
import com.google.common.util.concurrent.Futures;
import com.intellij.testFramework.LightProjectDescriptor;
import com.siyeh.ig.LightJavaInspectionTestCase;
import org.jetbrains.android.AndroidFacetProjectDescriptor;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("StatementWithEmptyBody")
public abstract class AndroidInspectionTestCase extends LightJavaInspectionTestCase {

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return AndroidFacetProjectDescriptor.INSTANCE;
  }

  protected void addManifest(int minApi) {
    myFixture.addFileToProject(SdkConstants.FN_ANDROID_MANIFEST_XML,
                               "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                               "    package=\"test.pkg.deprecation\">\n" +
                               "\n" +
                               "    <uses-sdk android:minSdkVersion=\"" + minApi + "\" />" +
                               "\n" +
                               "</manifest>\n");
    // Ensure that the MergedManifestManager has picked up the new manifest. Otherwise,
    // since we're using a shared project from a light project fixture, we might end up
    // with a stale merged manifest from another test case.
    Futures.getUnchecked(MergedManifestManager.getMergedManifest(myFixture.getModule()));
    MergedManifestModificationListener.ensureSubscribed(myFixture.getProject());
  }
}
