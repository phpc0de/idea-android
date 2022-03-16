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
package com.android.build.attribution.ui.controllers

import com.android.build.attribution.BuildAttributionWarningsFilter
import com.android.build.attribution.analyzers.IncompatiblePluginWarning
import com.android.build.attribution.data.GradlePluginsData
import com.android.build.attribution.data.StudioProvidedInfo
import com.android.build.attribution.ui.BuildAnalyzerBrowserLinks
import com.android.build.attribution.ui.analytics.BuildAttributionUiAnalytics
import com.android.build.attribution.ui.data.TaskUiData
import com.android.build.attribution.ui.model.BuildAnalyzerViewModel
import com.android.build.attribution.ui.model.TasksDataPageModel.Grouping
import com.android.build.attribution.ui.model.TasksFilter
import com.android.build.attribution.ui.model.TasksPageId
import com.android.build.attribution.ui.model.TasksTreeNode
import com.android.build.attribution.ui.model.WarningsFilter
import com.android.build.attribution.ui.model.WarningsPageId
import com.android.build.attribution.ui.model.WarningsTreeNode
import com.android.build.attribution.ui.view.ViewActionHandlers
import com.android.tools.idea.Projects
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel
import com.android.tools.idea.gradle.dsl.api.ProjectBuildModel
import com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames
import com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel
import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker
import com.android.tools.idea.gradle.project.build.invoker.GradleInvocationResult
import com.android.tools.idea.gradle.project.upgrade.performRecommendedPluginUpgrade
import com.android.tools.idea.memorysettings.MemorySettingsConfigurable
import com.google.common.base.Stopwatch
import com.google.wireless.android.sdk.stats.BuildAttributionUiEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import java.time.Duration

class BuildAnalyzerViewController(
  val model: BuildAnalyzerViewModel,
  private val project: Project,
  private val analytics: BuildAttributionUiAnalytics,
  private val issueReporter: TaskIssueReporter
) : ViewActionHandlers {

  init {
    analytics.initFirstPage(model)
  }

  override fun dataSetComboBoxSelectionUpdated(newSelectedData: BuildAnalyzerViewModel.DataSet) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration { model.selectedData = newSelectedData }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.DATA_VIEW_COMBO_SELECTED, duration)
  }

  override fun changeViewToTasksLinkClicked(targetGrouping: Grouping) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration {
      model.selectedData = BuildAnalyzerViewModel.DataSet.TASKS
      model.tasksPageModel.selectGrouping(targetGrouping)
    }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_LINK_CLICK, duration)
  }

  override fun changeViewToWarningsLinkClicked() {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration { model.selectedData = BuildAnalyzerViewModel.DataSet.WARNINGS }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_LINK_CLICK, duration)
  }

  override fun tasksGroupingSelectionUpdated(grouping: Grouping) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration { model.tasksPageModel.selectGrouping(grouping) }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.GROUPING_CHANGED, duration)
  }

  override fun tasksTreeNodeSelected(tasksTreeNode: TasksTreeNode?) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration { model.tasksPageModel.selectNode(tasksTreeNode) }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_TREE_CLICK, duration)
  }

  override fun tasksDetailsLinkClicked(taskPageId: TasksPageId) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration {
      // Make sure tasks page open.
      model.selectedData = BuildAnalyzerViewModel.DataSet.TASKS
      // Update selection in the tasks page model.
      model.tasksPageModel.selectPageById(taskPageId)
    }
    // Track page change in analytics.
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_LINK_CLICK, duration)
  }

  override fun warningsTreeNodeSelected(warningTreeNode: WarningsTreeNode?) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    // Update selection in the model.
    val duration = runAndMeasureDuration { model.warningsPageModel.selectNode(warningTreeNode) }
    // Track page change in analytics.
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_TREE_CLICK, duration)
  }

  override fun helpLinkClicked(linkTarget: BuildAnalyzerBrowserLinks) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    analytics.helpLinkClicked(currentAnalyticsPage, linkTarget)
  }

  override fun generateReportClicked(taskData: TaskUiData) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    analytics.bugReportLinkClicked(currentAnalyticsPage)
    issueReporter.reportIssue(taskData)
  }

  override fun openMemorySettings() {
    analytics.memorySettingsOpened()
    ShowSettingsUtil.getInstance().showSettingsDialog(project, MemorySettingsConfigurable::class.java)
  }

  override fun applyTasksFilter(filter: TasksFilter) {
    val duration = runAndMeasureDuration { model.tasksPageModel.applyFilter(filter) }
    analytics.tasksFilterApplied(filter, duration)
  }

  override fun applyWarningsFilter(filter: WarningsFilter) {
    val duration = runAndMeasureDuration { model.warningsPageModel.filter = filter }
    analytics.warningsFilterApplied(filter, duration)
  }

  override fun warningsGroupingSelectionUpdated(groupByPlugin: Boolean) {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration { model.warningsPageModel.groupByPlugin = groupByPlugin }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.GROUPING_CHANGED, duration)
  }

  override fun dontShowAgainNoGCSettingWarningClicked() {
    BuildAttributionWarningsFilter.getInstance(project).suppressNoGCSettingWarning = true
    analytics.noGCSettingWarningSuppressed()
  }

  override fun openConfigurationCacheWarnings() {
    val currentAnalyticsPage = analytics.getStateFromModel(model)
    val duration = runAndMeasureDuration {
      model.selectedData = BuildAnalyzerViewModel.DataSet.WARNINGS
      model.warningsPageModel.selectPageById(WarningsPageId.configurationCachingRoot)
    }
    val newAnalyticsPage = analytics.getStateFromModel(model)
    analytics.pageChange(currentAnalyticsPage, newAnalyticsPage, BuildAttributionUiEvent.EventType.PAGE_CHANGE_LINK_CLICK, duration)
  }

  override fun runAgpUpgrade() {
    ApplicationManager.getApplication().executeOnPooledThread { performRecommendedPluginUpgrade(project) }
    analytics.runAgpUpgradeClicked()
  }

  override fun runTestConfigurationCachingBuild() {
    ConfigurationCacheTestBuildFlowRunner.getInstance(project).startTestBuildsFlow()
    analytics.rerunBuildWithConfCacheClicked()
  }

  override fun turnConfigurationCachingOnInProperties() {
    StudioProvidedInfo.turnOnConfigurationCacheInProperties(project)
    analytics.turnConfigurationCacheOnInPropertiesClicked()
  }

  override fun updatePluginClicked(pluginWarningData: IncompatiblePluginWarning) {
    val duration = runAndMeasureDuration {
      val openFile = PluginVersionDeclarationFinder(project)
        .findFileToOpen(pluginWarningData.pluginInfo.pluginArtifact, pluginWarningData.plugin.displayNames())
      if (openFile?.canNavigate() == true) {
        openFile.navigate(true)
      }
    }
    analytics.updatePluginButtonClicked(duration)
  }

  private fun runAndMeasureDuration(action: () -> Unit): Duration {
    val watch = Stopwatch.createStarted()
    action()
    return watch.elapsed()
  }
}

class PluginVersionDeclarationFinder(val project: Project) {

  fun findFileToOpen(pluginArtifact: GradlePluginsData.DependencyCoordinates?, pluginDisplayNames: Set<String>): OpenFileDescriptor? {
    val buildModel = ProjectBuildModel.get(project)
    val rootBuildModel = buildModel.projectBuildModel
    val psiToOpen = findPluginDeclarationPsi(rootBuildModel, pluginArtifact, pluginDisplayNames)
    return if (psiToOpen != null) {
      OpenFileDescriptor(project, psiToOpen.containingFile.virtualFile, psiToOpen.textOffset)
    }
    else {
      rootBuildModel?.virtualFile?.let { OpenFileDescriptor(project, it, -1) }
    }
  }

  private fun findPluginDeclarationPsi(
    projectBuildModel: GradleBuildModel?,
    pluginArtifact: GradlePluginsData.DependencyCoordinates?,
    pluginDisplayNames: Set<String>
  ): PsiElement? {
    if (projectBuildModel == null) return null
    // Examine dependencies block
    val buildScriptDependenciesBlock = projectBuildModel.buildscript().dependencies()
    pluginArtifact?.run {
      buildScriptDependenciesBlock.artifacts(CommonConfigurationNames.CLASSPATH)
        .firstOrNull { name == it.name().forceString() && group == it.group().toString() }
        ?.psiElement?.let { return it }
    }

    // Examine plugins for plugin Dsl declarations.
    projectBuildModel.plugins().firstOrNull { plugin ->
      plugin.version().valueType == GradlePropertyModel.ValueType.STRING
      && pluginDisplayNames.contains(plugin.name().toString())
    }
      ?.psiElement?.let { return it }

    // TODO Support Plugin management case

    return buildScriptDependenciesBlock.psiElement
  }
}

class ConfigurationCacheTestBuildFlowRunner(val project: Project) {

  var runningTestConfigurationCacheBuild: Boolean = false
  var runningFirstConfigurationCacheBuild: Boolean = false

  companion object {
    fun getInstance(project: Project): ConfigurationCacheTestBuildFlowRunner {
      return ServiceManager.getService(project, ConfigurationCacheTestBuildFlowRunner::class.java)
    }
  }

  fun startTestBuildsFlow() {
    runFirstConfigurationCacheTestBuildWithConfirmation()
  }

  private val confirmationDialogHeader = "Configuration Cache Compatibility Assessment"

  private fun runFirstConfigurationCacheTestBuildWithConfirmation() {
    invokeLater(ModalityState.NON_MODAL) {
      val confirmationResult = Messages.showIdeaMessageDialog(
        project,
        """
          |This test will rerun the latest build twice to check that Gradle can serialize the task graph
          |and then reuse it from the cache. The builds will run in the background and they will fail
          |with details if any incompatibilities are detected.
        """.trimMargin(),
        confirmationDialogHeader,
        arrayOf("Run Builds", Messages.getCancelButton()), 0,
        Messages.getInformationIcon(), null
      )
      if (confirmationResult == Messages.OK) {
        scheduleRebuildWithCCOptionAndRunOnSuccess(firstBuild = true) {
          invokeLater { scheduleRebuildWithCCOptionAndRunOnSuccess(firstBuild = false) { showFinalSuccessMessage() } }
        }
      }
    }
  }

  private fun showFinalSuccessMessage() {
    invokeLater(ModalityState.NON_MODAL) {
      val confirmationResult = Messages.showIdeaMessageDialog(
        project,
        """
        |Both trial builds with Configuration cache on were successful. You can turn on
        |Configuration cache in gradle.properties.
        |
        |Note: Configuration cache is an experimental feature of Gradle and there may be
        |incompatibilities with different tasks’ runs in the future.
        """.trimMargin(),
        confirmationDialogHeader,
        arrayOf("Enable Configuration Cache", Messages.getCancelButton()), 0,
        Messages.getInformationIcon(), null
      )
      if (confirmationResult == Messages.OK) StudioProvidedInfo.turnOnConfigurationCacheInProperties(project)
    }
  }

  private fun scheduleRebuildWithCCOptionAndRunOnSuccess(firstBuild: Boolean, onSuccess: () -> Unit) {
    GradleBuildInvoker.getInstance(project).let { invoker ->
      invoker.add(object : GradleBuildInvoker.AfterGradleInvocationTask {
        override fun execute(result: GradleInvocationResult) {
          runningFirstConfigurationCacheBuild = false
          runningTestConfigurationCacheBuild = false
          invoker.remove(this)
          if (result.isBuildSuccessful) onSuccess()
          else showFailureMessage(result)
        }
      })
      invoker.rebuildWithTempOptions(Projects.getBaseDirPath(project), listOf("--configuration-cache"))
      runningFirstConfigurationCacheBuild = firstBuild
      runningTestConfigurationCacheBuild = true
    }
  }

  private fun showFailureMessage(result: GradleInvocationResult) {
    invokeLater(ModalityState.NON_MODAL) {
      if (result.isBuildCancelled) {
        Messages.showIdeaMessageDialog(
          project,
          "Build was cancelled.",
          confirmationDialogHeader,
          arrayOf(Messages.getOkButton()), 0,
          Messages.getInformationIcon(), null
        )
      }
      //TODO(b/186203445): we have configuration cache exception with a detailed message and a link to the html report inside.
      // So I can present that in the Dialog. find cause recursively?
      Messages.showIdeaMessageDialog(
        project,
        "Build failed. Please, check build output for a detailed report of incompatibilities detected by Gradle.",
        confirmationDialogHeader,
        arrayOf(Messages.getOkButton()), 0,
        Messages.getErrorIcon(), null
      )
    }
  }
}
