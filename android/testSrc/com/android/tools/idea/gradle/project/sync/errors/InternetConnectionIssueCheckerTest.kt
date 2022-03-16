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
import com.android.tools.idea.gradle.project.sync.quickFixes.ToggleOfflineModeQuickFix
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.google.common.truth.Truth.assertThat
import org.jetbrains.plugins.gradle.issue.GradleIssueData

class InternetConnectionIssueCheckerTest : AndroidGradleTestCase() {
  private val internetConnectionIssueChecker = InternetConnectionIssueChecker()

  fun testCheckIssue() {
    val issueData = GradleIssueData(projectFolderPath.path, Throwable("Network is unreachable"), null, null)
    val buildIssue = internetConnectionIssueChecker.check(issueData)

    assertThat(buildIssue).isNotNull()
    assertThat(buildIssue!!.description).contains("Network is unreachable")
    // Check QuickFix.
    assertThat(buildIssue.quickFixes).hasSize(1)
    assertThat(buildIssue.quickFixes[0]).isInstanceOf(ToggleOfflineModeQuickFix::class.java)
  }

  fun testCheckIssueHandled() {
    assertThat(
      internetConnectionIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "Could not GET ",
        null,
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(true)

    assertThat(
      internetConnectionIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "Could not HEAD ",
        null,
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(true)

    assertThat(
      internetConnectionIssueChecker.consumeBuildOutputFailureMessage(
        "Build failed with Exception",
        "Network is unreachable",
        null,
        null,
        "",
        TestMessageEventConsumer()
      )).isEqualTo(true)
  }
}