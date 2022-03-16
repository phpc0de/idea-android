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
package com.android.tools.idea.gradle.project.importing

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.Projects
import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.android.tools.idea.gradle.project.sync.SdkSync
import com.android.tools.idea.gradle.util.GradleUtil
import com.android.tools.idea.gradle.util.LocalProperties
import com.android.tools.idea.io.FilePaths
import com.android.tools.idea.sdk.IdeSdks
import com.android.tools.idea.util.ToolWindows
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectType
import com.intellij.openapi.project.ProjectTypeService
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.roots.CompilerProjectExtension
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.serviceContainer.NonInjectable
import com.intellij.util.ExceptionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.gradle.service.project.open.setupGradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleEnvironment
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * Imports an Android-Gradle project without showing the "Import Project" Wizard UI.
 */
class GradleProjectImporter @NonInjectable @VisibleForTesting internal constructor(
  private val mySdkSync: SdkSync,
  private val myTopLevelModuleFactory: TopLevelModuleFactory,
  private val myProjectFolderFactory: ProjectFolder.Factory
) {
  constructor() : this(SdkSync.getInstance(), TopLevelModuleFactory(), ProjectFolder.Factory())

  /**
   * Ensures presence of the top level Gradle build file and the .idea directory and, additionally, performs cleanup of the libraries
   * storage to force their re-import.
   */
  fun importAndOpenProjectCore(
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean,
    projectFolder: VirtualFile
  ): Project? {
    val projectFolderPath = VfsUtilCore.virtualToIoFile(projectFolder)
    try {
      setUpLocalProperties(projectFolderPath)
      val projectName = projectFolder.name
      val newProject = createProject(projectName, projectFolderPath)
      importProjectNoSync(Request(newProject))
      return ProjectManagerEx.getInstanceEx().openProject(
        projectFolderPath.toPath(),
        OpenProjectTask(
          forceOpenInNewFrame = forceOpenInNewFrame,
          projectToClose = projectToClose,
          isNewProject = false,
          useDefaultProjectAsTemplate = false,
          project = newProject,
          projectName = null,
          showWelcomeScreen = true,
          callback = null,
          line = -1,
          column = -1,
          isRefreshVfsNeeded = true,
          runConfigurators = false,
          runConversionBeforeOpen = true,
          projectWorkspaceId = null,
          isProjectCreatedWithWizard = false,
          beforeInit = null,
          beforeOpen = null,
          preparedToOpen = null
        )
      )
    }
    catch (e: Throwable) {
      if (ApplicationManager.getApplication().isUnitTestMode) {
        ExceptionUtil.rethrowUnchecked(e)
      }
      Messages.showErrorDialog(e.message, "Project Import")
      logger.error(e)
      return null
    }
  }

  @Throws(IOException::class)
  private fun setUpLocalProperties(projectFolderPath: File) {
    try {
      val localProperties = LocalProperties(projectFolderPath)
      if (IdeInfo.getInstance().isAndroidStudio) {
        mySdkSync.syncIdeAndProjectAndroidSdks(localProperties)
      }
    }
    catch (e: IOException) {
      logger.info("Failed to sync SDKs", e)
      Messages.showErrorDialog(e.message, "Project Import")
      throw e
    }
  }

  private val logger: Logger
    get() = Logger.getInstance(javaClass)

  @Throws(IOException::class)
  fun importProjectNoSync(request: Request) {
    val projectFolderPath = Projects.getBaseDirPath(request.project).absoluteFile
    val projectFolder = myProjectFolderFactory.create(projectFolderPath)
    projectFolder.createTopLevelBuildFile()
    projectFolder.createIdeaProjectFolder()
    val newProject = request.project
    val projectInfo = GradleProjectInfo.getInstance(newProject)
    projectInfo.isNewProject = request.isNewProject
    projectInfo.isImportedProject = true
    silenceUnlinkedGradleProjectNotificationIfNecessary(newProject)
    WriteAction.runAndWait<RuntimeException> {
      if (request.javaLanguageLevel != null) {
        val extension = LanguageLevelProjectExtension.getInstance(newProject)
        if (extension != null) {
          extension.languageLevel = request.javaLanguageLevel!!
        }
      }

      // In practice, it really does not matter where the compiler output folder is. Gradle handles that. This is done just to please
      // IDEA.
      val compilerOutputFolderPath = File(Projects.getBaseDirPath(newProject), FileUtil.join(GradleUtil.BUILD_DIR_DEFAULT_NAME, "classes"))
      val compilerOutputFolderUrl = FilePaths.pathToIdeaUrl(compilerOutputFolderPath)
      val compilerProjectExt = CompilerProjectExtension.getInstance(newProject)!!
      compilerProjectExt.setCompilerOutputUrl(compilerOutputFolderUrl)

      // This allows to customize UI when android project is opened inside IDEA with android plugin.
      ProjectTypeService.setProjectType(newProject, ANDROID_PROJECT_TYPE)
      myTopLevelModuleFactory.createTopLevelModule(newProject)
    }
    ExternalSystemUtil.invokeLater(newProject) { ToolWindows.activateProjectView(newProject) }
  }

  /**
   * Creates a new not configured project in a given location.
   */
  fun createProject(projectName: String, projectFolderPath: File): Project {
    GradleProjectInfo.beginInitializingGradleProjectAt(projectFolderPath).use { ignored ->
      val newProject = ProjectManagerEx.getInstanceEx().newProject(
        Path.of(projectFolderPath.path),
        OpenProjectTask(
          projectName = projectName
        )
      ) ?: throw NullPointerException("Failed to create a new project")
      configureNewProject(newProject)
      return newProject
    }
  }

  class Request(@JvmField val project: Project) {
    @JvmField
    var javaLanguageLevel: LanguageLevel? = null

    @JvmField
    var isNewProject = false
  }

  companion object {
    val ANDROID_PROJECT_TYPE = ProjectType("Android")

    // A copy of a private constant from GradleJvmStartupActivity.
    @NonNls
    private val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"

    @JvmStatic
    fun getInstance(): GradleProjectImporter = ServiceManager.getService(GradleProjectImporter::class.java)

    @VisibleForTesting
    @JvmStatic
    fun configureNewProject(newProject: Project) {
      // TODO(b/184826517): Enable `storeProjectFilesExternally` when the platform issue is fixed.
      val gradleSettings = GradleSettings.getInstance(newProject).also { it.storeProjectFilesExternally = false }
      val externalProjectPath = ExternalSystemApiUtil.toCanonicalPath(newProject.basePath!!)
      if (!gradleSettings.linkedProjectsSettings.isEmpty()) {
        check(ApplicationManager.getApplication().isUnitTestMode) { "configureNewProject should be used with new projects only" }
        for (setting in gradleSettings.linkedProjectsSettings) {
          gradleSettings.unlinkExternalProject(setting.externalProjectPath)
        }
      }
      val projectSettings = GradleProjectSettings()
      gradleSettings.setupGradleSettings()
      @Suppress("UnstableApiUsage")
      projectSettings.setupGradleProjectSettings(newProject, File(externalProjectPath).toPath())
      // Set gradleJvm to USE_PROJECT_JDK since this setting is only available in the PSD for Android Studio and use default jdk
      projectSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
      ExternalSystemApiUtil.getSettings(newProject, GradleConstants.SYSTEM_ID).linkProject(projectSettings)
      WriteAction.runAndWait<RuntimeException> {
        val jdk = IdeSdks.getInstance().jdk
        if (jdk != null) {
          ProjectRootManager.getInstance(newProject).projectSdk = jdk
        }
      }
    }

    private fun silenceUnlinkedGradleProjectNotificationIfNecessary(newProject: Project) {
      val gradleSettings = GradleSettings.getInstance(newProject)
      if (gradleSettings.linkedProjectsSettings.isEmpty()) {
        PropertiesComponent.getInstance(newProject).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
      }
    }
  }
}

// TODO(b/184826517): Remove when fixed. This is a temporary copy of
//  org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil.setupGradleSettings
//  which does not sets `storeProjectFilesExternally = true`.
private fun GradleSettings.setupGradleSettings() {
  gradleVmOptions = GradleEnvironment.Headless.GRADLE_VM_OPTIONS ?: gradleVmOptions
  isOfflineWork = GradleEnvironment.Headless.GRADLE_OFFLINE?.toBoolean() ?: isOfflineWork
  serviceDirectoryPath = GradleEnvironment.Headless.GRADLE_SERVICE_DIRECTORY ?: serviceDirectoryPath
}
