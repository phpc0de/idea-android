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

import com.android.SdkConstants
import com.android.ide.common.repository.GradleVersion
import com.android.tools.idea.gradle.plugin.LatestKnownPluginVersionProvider
import com.android.tools.idea.gradle.project.sync.idea.issues.BuildIssueComposer
import com.android.tools.idea.gradle.project.sync.idea.issues.updateUsageTracker
import com.android.tools.idea.gradle.project.sync.quickFixes.OpenPluginBuildFileQuickFix
import com.android.tools.idea.gradle.project.sync.quickFixes.UpgradeGradleVersionsQuickFix
import com.android.tools.idea.gradle.project.upgrade.AgpGradleVersionRefactoringProcessor
import com.android.tools.idea.gradle.project.upgrade.AgpGradleVersionRefactoringProcessor.Companion.getCompatibleGradleVersion
import com.android.tools.idea.sdk.IdeSdks
import com.google.wireless.android.sdk.stats.AndroidStudioEvent.GradleSyncFailure
import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.issue.BuildIssue
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.projectRoots.JavaSdkVersion
import org.gradle.tooling.UnsupportedVersionException
import org.jetbrains.plugins.gradle.issue.GradleIssueChecker
import org.jetbrains.plugins.gradle.issue.GradleIssueData
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler
import java.util.function.Consumer
import java.util.regex.Pattern

class OldAndroidPluginIssueChecker: GradleIssueChecker {
  companion object {
    private val PATTERN = Pattern.compile(
      "The android gradle plugin version .+ is too old, please update to the latest version.")
    private const val PLUGIN_TOO_OLD = "Plugin is too old, please update to a more recent version"
    private val UNSUPPORTED_GRADLE_VERSION_PATTERN = Pattern.compile(
      "Support for builds using Gradle versions older than (.*?) .* You are currently using Gradle version (.*?). .*", Pattern.DOTALL)
    val MINIMUM_AGP_VERSION_JDK_8 = GradleVersion(3, 1, 0)
    val MINIMUM_AGP_VERSION_JDK_11 = GradleVersion(3, 2, 0)
    val MINIMUM_GRADLE_VERSION = GradleVersion.parse(SdkConstants.GRADLE_MINIMUM_VERSION)
  }


  override fun check(issueData: GradleIssueData): BuildIssue? {
    val rootCause = GradleExecutionErrorHandler.getRootCauseAndLocation(issueData.error).first
    val message = rootCause.message ?: return null
    if (message.isBlank())
      return null
    var parsedMessage = tryToGetPluginTooOldMessage(message)
    var withMinimumVersion = false
    if (parsedMessage == null && rootCause is UnsupportedVersionException) {
      parsedMessage = tryToGetUnsupportedGradleMessage(message)
      withMinimumVersion = true
    }
    if (parsedMessage == null)
      return null

    // Log metrics.
    invokeLater {
      updateUsageTracker(issueData.projectPath, GradleSyncFailure.OLD_ANDROID_PLUGIN)
    }
    val composer = BuildIssueComposer(parsedMessage)
    if (withMinimumVersion) {
      val jdk = IdeSdks.getInstance().jdk
      val jdkVersion = if (jdk != null && jdk.versionString != null) JavaSdkVersion.fromVersionString(jdk.versionString!!) else null
      val isJdk8OrOlder = (jdkVersion != null) && (jdkVersion <= JavaSdkVersion.JDK_1_8)
      val minAgpToUse = if (isJdk8OrOlder) MINIMUM_AGP_VERSION_JDK_8 else MINIMUM_AGP_VERSION_JDK_11
      composer.addQuickFix(UpgradeGradleVersionsQuickFix(getCompatibleGradleVersion(minAgpToUse).version, minAgpToUse, "minimum"))
    }
    composer.addQuickFix(UpgradeGradleVersionsQuickFix(GradleVersion.parse(SdkConstants.GRADLE_LATEST_VERSION), GradleVersion.parse(LatestKnownPluginVersionProvider.INSTANCE.get()), "latest"))
    composer.addQuickFix("Open build file", OpenPluginBuildFileQuickFix())
    return composer.composeBuildIssue()
  }

  private fun tryToGetPluginTooOldMessage(message: String): String? {
    if (message.startsWith(PLUGIN_TOO_OLD) || PATTERN.matcher(message.lines()[0]).matches())
      return message
    return null
  }

  private fun tryToGetUnsupportedGradleMessage(message: String): String? {
    // Try to prevent updates to versions lower than 4.8.1 since they are not supported by AS 4.2+
    val matcher = UNSUPPORTED_GRADLE_VERSION_PATTERN.matcher(message)
    if (matcher.matches()) {
      val minimumVersion = GradleVersion.parse(matcher.group (1))
      val usedVersion = GradleVersion.parse(matcher.group(2))
      if (minimumVersion <= MINIMUM_GRADLE_VERSION) {
        return "This version of Android Studio requires projects to use Gradle $MINIMUM_GRADLE_VERSION or newer. This project is using Gradle $usedVersion."
      }
    }
    return null
  }

  override fun consumeBuildOutputFailureMessage(message: String,
                                                failureCause: String,
                                                stacktrace: String?,
                                                location: FilePosition?,
                                                parentEventId: Any,
                                                messageConsumer: Consumer<in BuildEvent>): Boolean {
    return tryToGetPluginTooOldMessage(failureCause) != null || tryToGetUnsupportedGradleMessage(failureCause) != null
  }
}