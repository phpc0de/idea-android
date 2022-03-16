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
package com.android.build.attribution.ui.model

import com.android.build.attribution.analyzers.AGPUpdateRequired
import com.android.build.attribution.analyzers.ConfigurationCacheCompatibilityTestFlow
import com.android.build.attribution.analyzers.ConfigurationCachingCompatibilityProjectResult
import com.android.build.attribution.analyzers.ConfigurationCachingTurnedOff
import com.android.build.attribution.analyzers.ConfigurationCachingTurnedOn
import com.android.build.attribution.analyzers.IncompatiblePluginWarning
import com.android.build.attribution.analyzers.IncompatiblePluginsDetected
import com.android.build.attribution.analyzers.NoIncompatiblePlugins
import com.android.build.attribution.ui.data.AnnotationProcessorUiData
import com.android.build.attribution.ui.data.AnnotationProcessorsReport
import com.android.build.attribution.ui.data.BuildAttributionReportUiData
import com.android.build.attribution.ui.data.TaskIssueType
import com.android.build.attribution.ui.data.TaskIssueUiData
import com.android.build.attribution.ui.data.TaskUiData
import com.android.build.attribution.ui.data.TimeWithPercentage
import com.android.build.attribution.ui.data.builder.TaskIssueUiDataContainer
import com.android.build.attribution.ui.view.BuildAnalyzerTreeNodePresentation
import com.android.build.attribution.ui.view.BuildAnalyzerTreeNodePresentation.NodeIconState
import com.android.build.attribution.ui.warningsCountString
import com.google.wireless.android.sdk.stats.BuildAttributionUiEvent.Page.PageType
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.tree.DefaultMutableTreeNode

interface WarningsDataPageModel {
  /** Text of the header visible above the tree. */
  val treeHeaderText: String

  /** The root of the tree that should be shown now. View is supposed to set this root in the Tree on update. */
  val treeRoot: DefaultMutableTreeNode

  var groupByPlugin: Boolean

  /** Currently selected node. Can be null in case of an empty tree. */
  val selectedNode: WarningsTreeNode?

  /** True if there are no warnings to show. */
  val isEmpty: Boolean

  var filter: WarningsFilter

  /**
   * Selects node in a tree to provided.
   * Provided node object should be the one created in this model and exist in the trees it holds.
   * Null means changing to empty selection.
   * Notifies listener if model state changes.
   */
  fun selectNode(warningsTreeNode: WarningsTreeNode?)

  /** Looks for the tree node by it's pageId and selects it as described in [selectNode] if found. */
  fun selectPageById(warningsPageId: WarningsPageId)

  /** Install the listener that will be called on model state changes. */
  fun addModelUpdatedListener(listener: (Boolean) -> Unit)

  /** Retrieve node descriptor by it's page id. Null if node does not exist in currently presented tree structure. */
  fun getNodeDescriptorById(pageId: WarningsPageId): WarningsTreePresentableNodeDescriptor?
}

class WarningsDataPageModelImpl(
  private val reportData: BuildAttributionReportUiData
) : WarningsDataPageModel {

  private val modelUpdatedListeners: MutableList<((treeStructureChanged: Boolean) -> Unit)> = CopyOnWriteArrayList()

  override val treeHeaderText: String
    get() = treeStructure.treeStats.let { treeStats ->
      "Warnings - Total: ${treeStats.totalWarningsCount}, Filtered: ${treeStats.filteredWarningsCount}"
    }

  override var filter: WarningsFilter = WarningsFilter.DEFAULT
    set(value) {
      field = value
      treeStructure.updateStructure(groupByPlugin, value)
      dropSelectionIfMissing()
      treeStructureChanged = true
      modelChanged = true
      notifyModelChanges()
    }

  override var groupByPlugin: Boolean = false
    set(value) {
      field = value
      treeStructure.updateStructure(value, filter)
      dropSelectionIfMissing()
      treeStructureChanged = true
      modelChanged = true
      notifyModelChanges()
    }

  private val treeStructure = WarningsTreeStructure(reportData).apply {
    updateStructure(groupByPlugin, filter)
  }

  override val treeRoot: DefaultMutableTreeNode
    get() = treeStructure.treeRoot

  // True when there are changes since last listener call.
  private var modelChanged = false

  // TODO (mlazeba): this starts look wrong. Can we provide TreeModel instead of a root node? what are the pros and cons? what are the other options?
  //   idea 1) make listener have multiple methods: tree updated, selection updated, etc.
  //   idea 2) provide TreeModel instead of root. then that model updates tree itself on changes.
  // True when tree changed it's structure since last listener call.
  private var treeStructureChanged = false

  private var selectedPageId: WarningsPageId = WarningsPageId.emptySelection
    private set(value) {
      if (value != field) {
        field = value
        modelChanged = true
      }
    }
  override val selectedNode: WarningsTreeNode?
    get() = treeStructure.pageIdToNode[selectedPageId]

  override val isEmpty: Boolean
    get() = reportData.issues.sumBy { it.warningCount } +
      reportData.annotationProcessors.issueCount +
      reportData.confCachingData.warningsCount() == 0

  override fun selectNode(warningsTreeNode: WarningsTreeNode?) {
    selectedPageId = warningsTreeNode?.descriptor?.pageId ?: WarningsPageId.emptySelection
    notifyModelChanges()
  }

  override fun selectPageById(warningsPageId: WarningsPageId) {
    treeStructure.pageIdToNode[warningsPageId]?.let { selectNode(it) }
  }

  override fun addModelUpdatedListener(listener: (Boolean) -> Unit) {
    modelUpdatedListeners.add(listener)
  }

  override fun getNodeDescriptorById(pageId: WarningsPageId): WarningsTreePresentableNodeDescriptor? =
    treeStructure.pageIdToNode[pageId]?.descriptor

  private fun dropSelectionIfMissing() {
    if (!treeStructure.pageIdToNode.containsKey(selectedPageId)) {
      selectedPageId = WarningsPageId.emptySelection
    }
  }

  private fun notifyModelChanges() {
    if (modelChanged) {
      modelUpdatedListeners.forEach { it.invoke(treeStructureChanged) }
      modelChanged = false
      treeStructureChanged = false
    }
  }
}

private class WarningsTreeStructure(
  val reportData: BuildAttributionReportUiData
) {

  val pageIdToNode: MutableMap<WarningsPageId, WarningsTreeNode> = mutableMapOf()

  val treeStats: TreeStats = TreeStats()

  private fun treeNode(descriptor: WarningsTreePresentableNodeDescriptor) = WarningsTreeNode(descriptor).apply {
    pageIdToNode[descriptor.pageId] = this
  }

  var treeRoot = DefaultMutableTreeNode()

  fun updateStructure(groupByPlugin: Boolean, filter: WarningsFilter) {
    pageIdToNode.clear()
    treeStats.clear()
    treeRoot.let { rootNode ->
      rootNode.removeAllChildren()
      val taskWarnings = reportData.issues.asSequence()
        .flatMap { it.issues.asSequence() }
        .filter { filter.acceptTaskIssue(it) }
        .toList()
      treeStats.filteredWarningsCount += taskWarnings.size

      if (groupByPlugin) {
        taskWarnings.groupBy { it.task.pluginName }.forEach { (pluginName, warnings) ->
          val warningsByTask = warnings.groupBy { it.task }
          val pluginTreeGroupingNode = treeNode(PluginGroupingWarningNodeDescriptor(pluginName, warningsByTask))
          rootNode.add(pluginTreeGroupingNode)
          warningsByTask.forEach { (task, warnings) ->
            pluginTreeGroupingNode.add(treeNode(TaskUnderPluginDetailsNodeDescriptor(task, warnings)))
          }
        }
      }
      else {
        taskWarnings.groupBy { it.type }.forEach { (type, warnings) ->
          val warningTypeGroupingNodeDescriptor = TaskWarningTypeNodeDescriptor(type, warnings)
          val warningTypeGroupingNode = treeNode(warningTypeGroupingNodeDescriptor)
          rootNode.add(warningTypeGroupingNode)
          warnings.map { TaskWarningDetailsNodeDescriptor(it) }.forEach { taskIssueNodeDescriptor ->
            warningTypeGroupingNode.add(treeNode(taskIssueNodeDescriptor))
          }
        }
      }
      treeStats.totalWarningsCount += reportData.issues.sumBy { it.warningCount }

      if (filter.showAnnotationProcessorWarnings) {
        reportData.annotationProcessors.nonIncrementalProcessors.asSequence()
          .map { AnnotationProcessorDetailsNodeDescriptor(it) }
          .toList()
          .ifNotEmpty {
            val annotationProcessorsRootNode = treeNode(AnnotationProcessorsRootNodeDescriptor(reportData.annotationProcessors))
            rootNode.add(annotationProcessorsRootNode)
            forEach {
              annotationProcessorsRootNode.add(treeNode(it))
            }
            treeStats.filteredWarningsCount += size
          }
      }
      treeStats.totalWarningsCount += reportData.annotationProcessors.issueCount

      // Add configuration caching issues
      if (filter.showConfigurationCacheWarnings && reportData.confCachingData.shouldShowWarning()) {
        val configurationDuration = reportData.buildSummary.configurationDuration
        val configurationCacheData = reportData.confCachingData
        rootNode.add(treeNode(ConfigurationCachingRootNodeDescriptor(configurationCacheData, configurationDuration)).apply {
          if (configurationCacheData is IncompatiblePluginsDetected) {
            configurationCacheData.upgradePluginWarnings.forEach {
              add(treeNode(ConfigurationCachingWarningNodeDescriptor(it, configurationDuration)))
            }
            configurationCacheData.incompatiblePluginWarnings.forEach {
              add(treeNode(ConfigurationCachingWarningNodeDescriptor(it, configurationDuration)))
            }
          }
        })
        treeStats.filteredWarningsCount += configurationCacheData.warningsCount()
      }
      treeStats.totalWarningsCount += reportData.confCachingData.warningsCount()
    }
  }

  class TreeStats {
    var totalWarningsCount: Int = 0
    var filteredWarningsCount: Int = 0

    fun clear() {
      totalWarningsCount = 0
      filteredWarningsCount = 0
    }
  }
}

class WarningsTreeNode(
  val descriptor: WarningsTreePresentableNodeDescriptor
) : DefaultMutableTreeNode(descriptor)

// TODO (mlazeba): consider removing this class as it is not really used
enum class WarningsPageType {
  EMPTY_SELECTION,
  TASK_WARNING_DETAILS,
  TASK_WARNING_TYPE_GROUP,
  TASK_UNDER_PLUGIN,
  TASK_WARNING_PLUGIN_GROUP,
  ANNOTATION_PROCESSOR_DETAILS,
  ANNOTATION_PROCESSOR_GROUP,
  CONFIGURATION_CACHING_ROOT,
  CONFIGURATION_CACHING_WARNING,
}

data class WarningsPageId(
  val pageType: WarningsPageType,
  val id: String
) {
  companion object {
    fun warning(warning: TaskIssueUiData) =
      WarningsPageId(WarningsPageType.TASK_WARNING_DETAILS, "${warning.type}-${warning.task.taskPath}")

    fun task(task: TaskUiData) = WarningsPageId(WarningsPageType.TASK_UNDER_PLUGIN, task.taskPath)

    fun warningType(warningType: TaskIssueType) = WarningsPageId(WarningsPageType.TASK_WARNING_TYPE_GROUP, warningType.name)
    fun warningPlugin(warningPluginName: String) = WarningsPageId(WarningsPageType.TASK_WARNING_PLUGIN_GROUP, warningPluginName)

    fun annotationProcessor(annotationProcessorData: AnnotationProcessorUiData) = WarningsPageId(
      WarningsPageType.ANNOTATION_PROCESSOR_DETAILS, annotationProcessorData.className)

    fun configurationCachingWarning(data: IncompatiblePluginWarning) =
      WarningsPageId(WarningsPageType.CONFIGURATION_CACHING_WARNING, data.plugin.toString())

    val annotationProcessorRoot = WarningsPageId(WarningsPageType.ANNOTATION_PROCESSOR_GROUP, "ANNOTATION_PROCESSORS")
    val configurationCachingRoot = WarningsPageId(WarningsPageType.CONFIGURATION_CACHING_ROOT, "CONFIGURATION_CACHING")
    val emptySelection = WarningsPageId(WarningsPageType.EMPTY_SELECTION, "EMPTY")
  }
}

sealed class WarningsTreePresentableNodeDescriptor {
  abstract val pageId: WarningsPageId
  abstract val analyticsPageType: PageType
  abstract val presentation: BuildAnalyzerTreeNodePresentation
  override fun toString(): String = presentation.mainText
}

/** Descriptor for the task warning type group node. */
class TaskWarningTypeNodeDescriptor(
  val warningType: TaskIssueType,
  val presentedWarnings: List<TaskIssueUiData>

) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.warningType(warningType)
  override val analyticsPageType = when (warningType) {
    TaskIssueType.ALWAYS_RUN_TASKS -> PageType.ALWAYS_RUN_ISSUE_ROOT
    TaskIssueType.TASK_SETUP_ISSUE -> PageType.TASK_SETUP_ISSUE_ROOT
  }

  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = warningType.uiName,
      suffix = warningsCountString(presentedWarnings.size),
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(presentedWarnings.sumByLong { it.task.executionTime.timeMs })
    )
}

/** Descriptor for the task warning page node. */
class TaskWarningDetailsNodeDescriptor(
  val issueData: TaskIssueUiData
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.warning(issueData)
  override val analyticsPageType = when (issueData) {
    is TaskIssueUiDataContainer.TaskSetupIssue -> PageType.TASK_SETUP_ISSUE_PAGE
    is TaskIssueUiDataContainer.AlwaysRunNoOutputIssue -> PageType.ALWAYS_RUN_NO_OUTPUTS_PAGE
    is TaskIssueUiDataContainer.AlwaysRunUpToDateOverride -> PageType.ALWAYS_RUN_UP_TO_DATE_OVERRIDE_PAGE
    else -> PageType.UNKNOWN_PAGE
  }
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = issueData.task.taskPath,
      nodeIconState = NodeIconState.WARNING_ICON,
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(issueData.task.executionTime.timeMs)
    )
}

class PluginGroupingWarningNodeDescriptor(
  val pluginName: String,
  val presentedTasksWithWarnings: Map<TaskUiData, List<TaskIssueUiData>>

) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.warningPlugin(pluginName)

  override val analyticsPageType = PageType.PLUGIN_WARNINGS_ROOT

  private val warningsCount = presentedTasksWithWarnings.values.sumBy { it.size }

  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = pluginName,
      suffix = warningsCountString(warningsCount),
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(presentedTasksWithWarnings.keys.sumByLong { it.executionTime.timeMs })
    )
}

/** Descriptor for the task warning page node. */
class TaskUnderPluginDetailsNodeDescriptor(
  val taskData: TaskUiData,
  val filteredWarnings: List<TaskIssueUiData>
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.task(taskData)
  override val analyticsPageType = PageType.PLUGIN_TASK_WARNINGS
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = taskData.taskPath,
      nodeIconState = NodeIconState.WARNING_ICON,
      suffix = warningsCountString(filteredWarnings.size),
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(taskData.executionTime.timeMs)
    )
}

/** Descriptor for the non-incremental annotation processors group node. */
class AnnotationProcessorsRootNodeDescriptor(
  val annotationProcessorsReport: AnnotationProcessorsReport
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.annotationProcessorRoot
  override val analyticsPageType = PageType.ANNOTATION_PROCESSORS_ROOT
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = "Non-incremental Annotation Processors",
      suffix = warningsCountString(annotationProcessorsReport.issueCount)
    )
}

/** Descriptor for the non-incremental annotation processor page node. */
class AnnotationProcessorDetailsNodeDescriptor(
  val annotationProcessorData: AnnotationProcessorUiData
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.annotationProcessor(annotationProcessorData)
  override val analyticsPageType = PageType.ANNOTATION_PROCESSOR_PAGE
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = annotationProcessorData.className,
      nodeIconState = NodeIconState.WARNING_ICON,
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(annotationProcessorData.compilationTimeMs)
    )
}

/** Descriptor for the configuration caching problems page node. */
class ConfigurationCachingRootNodeDescriptor(
  val data: ConfigurationCachingCompatibilityProjectResult,
  val projectConfigurationTime: TimeWithPercentage
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.configurationCachingRoot
  override val analyticsPageType = PageType.CONFIGURATION_CACHE_ROOT
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = "Configuration cache",
      suffix = when (data) {
        is AGPUpdateRequired -> "Android Gradle plugin update required"
        is IncompatiblePluginsDetected -> data.upgradePluginWarnings.size.let {
          when (it) {
            0 -> ""
            1 -> "1 plugin requires update"
            else -> "$it plugins require update"
          }
        }
        is NoIncompatiblePlugins -> ""
        ConfigurationCachingTurnedOn -> ""
        ConfigurationCacheCompatibilityTestFlow -> ""
        ConfigurationCachingTurnedOff -> ""
      },
      rightAlignedSuffix = rightAlignedNodeDurationTextFromMs(projectConfigurationTime.timeMs)
    )
}

class ConfigurationCachingWarningNodeDescriptor(
  val data: IncompatiblePluginWarning,
  val projectConfigurationTime: TimeWithPercentage
) : WarningsTreePresentableNodeDescriptor() {
  override val pageId: WarningsPageId = WarningsPageId.configurationCachingWarning(data)
  override val analyticsPageType = PageType.CONFIGURATION_CACHE_PLUGIN_WARNING
  override val presentation: BuildAnalyzerTreeNodePresentation
    get() = BuildAnalyzerTreeNodePresentation(
      mainText = data.plugin.displayName,
      suffix = if (data.requiredVersion != null) "update required" else "not compatible",
      nodeIconState = NodeIconState.WARNING_ICON
    )
}

private fun ConfigurationCachingCompatibilityProjectResult.warningsCount() = when (this) {
  is AGPUpdateRequired -> 1
  is IncompatiblePluginsDetected -> incompatiblePluginWarnings.size + upgradePluginWarnings.size
  is NoIncompatiblePlugins -> 1
  ConfigurationCacheCompatibilityTestFlow -> 1
  ConfigurationCachingTurnedOn -> 0
  ConfigurationCachingTurnedOff -> 0
}

fun ConfigurationCachingCompatibilityProjectResult.shouldShowWarning(): Boolean = warningsCount() != 0

private fun rightAlignedNodeDurationTextFromMs(timeMs: Long) =
  if (timeMs >= 100) "%.1fs".format(timeMs.toDouble() / 1000) else "<0.1s"