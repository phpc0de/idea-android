/*
 * Copyright (C) 2019 The Android Open Source Project
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
@file:JvmName("BuildAttributionUtil")

package com.android.tools.idea.gradle.project.build.attribution

import com.android.SdkConstants.DOT_GRADLE
import com.android.ide.common.repository.GradleVersion
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.gradle.project.ProjectStructure
import com.android.utils.FileUtils
import com.intellij.openapi.project.Project
import java.io.File

private val minimumSupportedAgpVersion = GradleVersion.tryParseAndroidGradlePluginVersion("4.0.0-beta05")!!

fun isBuildAttributionEnabledForProject(project: Project): Boolean {
  return StudioFlags.BUILD_ATTRIBUTION_ENABLED.get()
         && isAgpVersionHigherOrEqualToMinimal(project)
}

fun isAgpVersionHigherOrEqualToMinimal(project: Project): Boolean {
  return ProjectStructure.getInstance(project).androidPluginVersions.allVersions.all { it.higherOrEqualToMinimal() }
}

private fun GradleVersion.higherOrEqualToMinimal() = compareTo(minimumSupportedAgpVersion) >= 0

fun buildOutputLine(): String = BuildAttributionOutputLinkFilter.INSIGHTS_AVAILABLE_LINE

fun getAgpAttributionFileDir(buildDir: File): File {
  // $projectDir/.gradle
  return FileUtils.join(buildDir, DOT_GRADLE)
}