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
package com.android.tools.idea.gradle.project.sync.idea.issues

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.project.sync.SimulatedSyncErrors
import com.android.tools.idea.gradle.project.sync.issues.TestSyncIssueUsageReporter.Companion.replaceSyncMessagesService
import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.android.tools.idea.testing.IdeComponents
import com.android.tools.idea.testing.TestProjectPaths
import com.google.common.truth.Truth.assertThat
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.GradleSyncFailure
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy

class Jdk8RequiredErrorTest : AndroidGradleTestCase() {
  fun testJdk8RequiredError() {
    loadProject(TestProjectPaths.SIMPLE_APPLICATION)
    val ideSdks = spy(IdeSdks.getInstance())
    IdeComponents(project).replaceApplicationService<IdeSdks>(IdeSdks::class.java, ideSdks)
    `when`(ideSdks.isUsingJavaHomeJdk).thenReturn(false)
    val usageReporter = replaceSyncMessagesService(project, testRootDisposable)
    SimulatedSyncErrors.registerSyncErrorToSimulate(
      "com/android/jack/api/ConfigNotSupportedException : Unsupported major.minor version 52.0")
    val message: String = requestSyncAndGetExpectedFailure()
    val expectedText = StringBuilder("com/android/jack/api/ConfigNotSupportedException : Unsupported major.minor version 52.0\n")
    expectedText.append("Please use JDK 8 or newer.\n")
    val androidStudio = IdeInfo.getInstance().isAndroidStudio

    // Verify hyperlinks are correct.
    if (androidStudio) { // Android Studio has extra quick-fix
      expectedText.append("<a href=\"use.java.home.as.jdk\">Set Android Studio to use the same JDK as Gradle and sync project</a>\n")
    }
    expectedText.append("<a href=\"select.jdk.from.new.psd\">Select a JDK from the File System</a>\n" +
                        "<a href=\"download.jdk8\">Download JDK 8</a>")
    assertThat(message).contains(expectedText.toString())
    assertEquals(GradleSyncFailure.JDK8_REQUIRED, usageReporter.collectedFailure)
  }
}