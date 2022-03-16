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
package com.android.tools.idea.gradle.project.sync.errors

import com.android.tools.idea.gradle.project.build.output.TestMessageEventConsumer
import com.android.tools.idea.gradle.project.sync.quickFixes.OpenFileAtLocationQuickFix
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.google.common.truth.Truth.assertThat
import org.jetbrains.plugins.gradle.issue.GradleIssueData

class MissingAndroidSdkIssueCheckerTest : AndroidGradleTestCase() {
  private val missingAndroidSdkIssueChecker = MissingAndroidSdkIssueChecker()

  fun testCheckIssue() {
    loadSimpleApplication()
    val issueData = GradleIssueData(projectFolderPath.path, RuntimeException("No sdk.dir property defined in local.properties file."), null,
                                    null)
    val buildIssue = missingAndroidSdkIssueChecker.check(issueData)

    assertThat(buildIssue).isNotNull()
    assertThat(buildIssue!!.description).contains("No sdk.dir property defined in local.properties file.\nPlease fix the 'sdk.dir' property in the local.properties file.")
    // Verify quickFix
    assertThat(buildIssue.quickFixes).hasSize(1)
    assertThat(buildIssue.quickFixes[0]).isInstanceOf(OpenFileAtLocationQuickFix::class.java)
  }

  fun testCheckIssueHandled() {
    assertThat(
      missingAndroidSdkIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "No sdk.dir property defined in local.properties file.",
        "Caused by: java.lang.RuntimeException",
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(true)

    assertThat(
      missingAndroidSdkIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "The SDK directory '/xyz/foo/' does not exist.",
        "Caused by: java.lang.RuntimeException",
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(true)

    assertThat(
      missingAndroidSdkIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "The SDK directory '/xyz/foo/' does not exist.",
        "Caused by: java.net.SocketException",
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(false)
  }
}