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
package com.android.tools.idea.templates

import com.android.ide.common.repository.GradleVersion
import com.android.repository.Revision
import com.android.tools.idea.gradle.plugin.AndroidPluginInfo
import com.android.tools.idea.gradle.plugin.LatestKnownPluginVersionProvider
import com.android.tools.idea.gradle.util.GradleUtil
import com.android.tools.idea.npw.module.ConfigureAndroidModuleStep
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.sdk.progress.StudioLoggerProgressIndicator
import com.android.tools.idea.wizard.template.FormFactor
import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.PackageName
import com.android.tools.idea.wizard.template.ProjectTemplateData
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.android.refactoring.isAndroidx
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File

val log: Logger get() = logger<ProjectTemplateDataBuilder>()

/**
 * Builder for [ProjectTemplateData].
 *
 * Extracts information from various data sources.
 */

const val DEFAULT_KOTLIN_VERSION = "1.4.31"

class ProjectTemplateDataBuilder(val isNewProject: Boolean) {
  var androidXSupport: Boolean? = null
  var gradlePluginVersion: GradleVersion? = null
  var sdkDir: File? = null
  var language: Language? = null
  var kotlinVersion: String? = null
  var buildToolsVersion: Revision? = null
  var explicitBuildToolsVersion: Boolean? = null
  var topOut: File? = null
  var applicationPackage: PackageName? = null
  val includedFormFactorNames = mutableMapOf<FormFactor, MutableList<String>>()
  var debugKeyStoreSha1: String? = null
  var overridePathCheck: Boolean? = null
  var applicationName: String? = null

  internal fun setEssentials(project: Project) {
    applicationName = project.name
    kotlinVersion = getBestKotlinVersion()
    gradlePluginVersion = determineGradlePluginVersion(project)
    // If we create a new project, then we have a checkbox for androidX support
    if (!isNewProject) {
      androidXSupport = project.isAndroidx()
    }
  }

  private fun getBestKotlinVersion() : String {
    // From https://github.com/JetBrains/intellij-kotlin/blob/master/project-wizard/idea/src/org/jetbrains/kotlin/tools/projectWizard/wizard/service/IdeaKotlinVersionProviderService.kt
    val kotlinVersionFromCompiler = KotlinCompilerVersion.getVersion()
      ?.takeUnless { it.contains("snapshot", ignoreCase = true) }
      ?.substringBefore("-release")

    return kotlinVersionFromCompiler ?: DEFAULT_KOTLIN_VERSION // The default version will only be used as a fallback
  }

  /**
   * Sets basic information which is available in [Project].
   */
  fun setProjectDefaults(project: Project) {
    setEssentials(project)

    val basePath = project.basePath
    if (basePath != null) {
      topOut = File(basePath)
    }

    val sdkHandler = AndroidSdks.getInstance().tryToChooseSdkHandler()
    val progress = StudioLoggerProgressIndicator(ConfigureAndroidModuleStep::class.java)

    sdkDir = sdkHandler.location?.toFile()
  }

  /** Find the most appropriated Gradle Plugin version for the specified project. */
  private fun determineGradlePluginVersion(project: Project?): GradleVersion {
    val defaultGradleVersion = GradleVersion.parse(LatestKnownPluginVersionProvider.INSTANCE.get())
    if (project == null || isNewProject) {
      return defaultGradleVersion
    }

    val versionInUse = GradleUtil.getAndroidGradleModelVersionInUse(project)
    val androidPluginInfo = AndroidPluginInfo.findFromBuildFiles(project)
    return versionInUse ?: androidPluginInfo?.pluginVersion ?: defaultGradleVersion
  }

  fun build() = ProjectTemplateData(
    androidXSupport!!,
    gradlePluginVersion!!.toString(),
    sdkDir,
    Language.valueOf(language!!.toString()),
    kotlinVersion!!,
    topOut!!,
    applicationPackage,
    includedFormFactorNames,
    debugKeyStoreSha1,
    overridePathCheck,
    isNewProject
  )
}

