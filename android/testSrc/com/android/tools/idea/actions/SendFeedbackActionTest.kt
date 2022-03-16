/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.actions

import com.android.tools.idea.gradle.project.AndroidStudioGradleInstallationManager
import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.projectRoots.JavaSdk
import org.junit.Test

/**
 * Tests for [SendFeedbackAction]
 */
class SendFeedbackActionTest: AndroidGradleTestCase() {

  /**
   * Verify that Gradle JDK information is used.
   */
  @Test
  fun testDescriptionContainsGradleJdk() {
    loadSimpleApplication()
    val description = SendFeedbackAction.getDescription(project)
    val jdkPath = AndroidStudioGradleInstallationManager.getInstance().getGradleJvmPath(project, project.basePath!!)
    assertThat(jdkPath).isNotNull()

    val jdkVersion = JavaSdk.getInstance().getVersionString(jdkPath)
    assertThat(description).contains("Gradle JDK: ${jdkVersion}")
    assertThat(description).doesNotContain("Gradle JDK: (default)")
  }

  /**
   * Verify that the default Gradle JDK is used when project is null
   */
  @Test
  fun testDescriptionContainsDefaultGradleJdk() {
    val description = SendFeedbackAction.getDescription(null)
    val jdk = IdeSdks.getInstance().jdk
    assertThat(jdk).isNotNull()
    assertThat(description).contains("Gradle JDK: (default) ${jdk!!.versionString}")
  }
}