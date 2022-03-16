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

import com.android.build.attribution.ui.MockUiData
import com.android.build.attribution.ui.data.PluginSourceType
import com.android.build.attribution.ui.data.builder.TaskIssueUiDataContainer
import com.android.build.attribution.ui.mockTask
import com.android.build.attribution.ui.model.TasksDataPageModel.Grouping
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TasksDataPageModelImplTest {

  val task1 = mockTask(":app", "compile", "compiler.plugin", 2000)
  val task2 = mockTask(":app", "resources", "resources.plugin", 1000)
  val task3 = mockTask(":lib", "compile", "compiler.plugin", 1000)

  val mockData = MockUiData(tasksList = listOf(task1, task2, task3))

  var modelUpdateListenerCallsCount = 0
  var modelUpdateListenerCallsWithTreeUpdateCount = 0
  val model: TasksDataPageModel = TasksDataPageModelImpl(mockData).apply {
    addModelUpdatedListener { treeUpdated ->
      modelUpdateListenerCallsCount++
      if (treeUpdated) modelUpdateListenerCallsWithTreeUpdateCount++
    }
  }

  @Test
  fun testInitialSelection() {
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(0)
  }

  @Test
  fun testGroupingChangeToPlugins() {
    // Act
    model.selectGrouping(Grouping.BY_PLUGIN)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.BY_PLUGIN)
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  compiler.plugin
      |    :app:compile
      |    :lib:compile
      |  resources.plugin
      |    :app:resources
    """.trimMargin())
    // Update should trigger model update listener once
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(1)
  }

  @Test
  fun testGroupingChangeToUngrouped() {
    // Arrange
    model.selectGrouping(Grouping.BY_PLUGIN)
    modelUpdateListenerCallsCount = 0
    modelUpdateListenerCallsWithTreeUpdateCount = 0

    // Act
    model.selectGrouping(Grouping.UNGROUPED)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(1)
  }

  @Test
  fun testSelectingSameGroupingDoesNotTriggerListener() {
    // Arrange
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)

    // Act
    model.selectGrouping(Grouping.UNGROUPED)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)
    assertThat(modelUpdateListenerCallsCount).isEqualTo(0)
  }

  @Test
  fun testSelectNode() {
    // Arrange
    val lastChild = model.treeRoot.lastChild as TasksTreeNode

    // Act
    model.selectNode(lastChild)

    // Assert
    assertThat(model.selectedNode).isEqualTo(lastChild)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |=>:lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(0)
  }

  @Test
  fun testDeselectNode() {
    // Arrange
    val lastChild = model.treeRoot.lastChild as TasksTreeNode
    model.selectNode(lastChild)

    // Act
    model.selectNode(null)

    // Assert
    assertThat(model.selectedNode).isNull()
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(2)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(0)
  }

  @Test
  fun testTreeKeepsSelectionWhenChangeToPlugins() {
    // Arrange
    model.selectNode(model.treeRoot.firstChild as TasksTreeNode)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |=>:app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    modelUpdateListenerCallsCount = 0

    // Act
    model.selectGrouping(Grouping.BY_PLUGIN)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.BY_PLUGIN)
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  compiler.plugin
      |===>:app:compile
      |    :lib:compile
      |  resources.plugin
      |    :app:resources
    """.trimMargin())
    // Update should trigger model update listener once
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
  }

  @Test
  fun testTreeKeepsSelectionWhenChangeToUngrouped() {
    // Arrange
    model.selectGrouping(Grouping.BY_PLUGIN)
    model.selectNode(model.treeRoot.lastLeaf as TasksTreeNode)
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  compiler.plugin
      |    :app:compile
      |    :lib:compile
      |  resources.plugin
      |===>:app:resources
    """.trimMargin())
    modelUpdateListenerCallsCount = 0

    // Act
    model.selectGrouping(Grouping.UNGROUPED)

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |=>:app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
  }

  @Test
  fun testTreeDropsSelectionWhenChangeToUngroupedWhilePluginSelected() {
    // Arrange
    model.selectGrouping(Grouping.BY_PLUGIN)
    model.selectNode(model.treeRoot.lastChild as TasksTreeNode)
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  compiler.plugin
      |    :app:compile
      |    :lib:compile
      |=>resources.plugin
      |    :app:resources
    """.trimMargin())
    modelUpdateListenerCallsCount = 0

    // Act
    model.selectGrouping(Grouping.UNGROUPED)

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
  }

  @Test
  fun testSelectNodeFromDifferentGrouping() {
    // Arrange
    val lastUngroupedNode = model.treeRoot.lastChild as TasksTreeNode
    model.selectGrouping(Grouping.BY_PLUGIN)
    modelUpdateListenerCallsCount = 0
    modelUpdateListenerCallsWithTreeUpdateCount = 0

    // Act
    model.selectNode(lastUngroupedNode)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |=>:lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(1)
  }

  @Test
  fun testSelectByPageId() {
    // Act
    val pageId = TasksPageId.task(task3, Grouping.UNGROUPED)
    model.selectPageById(pageId)

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |=>:lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(0)
  }

  @Test
  fun testSelectByPageIdFromDifferentGrouping() {
    // Act
    val pageId = TasksPageId.task(task3, Grouping.BY_PLUGIN)
    model.selectPageById(pageId)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.BY_PLUGIN)
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  compiler.plugin
      |    :app:compile
      |===>:lib:compile
      |  resources.plugin
      |    :app:resources
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(1)
  }

  @Test
  fun testSelectByNotExistingPageId() {
    // Act
    val nonExistingPageId = TasksPageId(Grouping.UNGROUPED, TaskDetailsPageType.TASK_DETAILS, "does-not-exist")
    model.selectPageById(nonExistingPageId)

    // Assert
    assertThat(model.selectedGrouping).isEqualTo(Grouping.UNGROUPED)
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 4.0s
      |ROOT
      |  :app:compile
      |  :app:resources
      |  :lib:compile
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(0)
  }

  @Test
  fun testFilterApplySelectedNodeRemains() {
    // Arrange
    val task1 = mockTask(":app", "compile", "compiler.plugin", 2000).apply {
      issues = listOf(TaskIssueUiDataContainer.AlwaysRunNoOutputIssue(this))
    }
    val task2 = mockTask(":app", "resources", "resources.plugin", 1000)
    val task3 = mockTask(":lib", "compile", "compiler.plugin", 1000).apply {
      issues = listOf(TaskIssueUiDataContainer.AlwaysRunNoOutputIssue(this))
    }

    val mockData = MockUiData(tasksList = listOf(task1, task2, task3))
    val model = TasksDataPageModelImpl(mockData)

    model.selectNode(model.treeRoot.firstLeaf as TasksTreeNode)

    // Act - apply filter
    model.applyFilter(TasksFilter.DEFAULT.copy(showTasksWithoutWarnings = false))

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 3.0s - 2 Warnings
      |ROOT
      |=>:app:compile
      |  :lib:compile
    """.trimMargin())

    // Act - group by plugin
    model.selectGrouping(Grouping.BY_PLUGIN)

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 3.0s - 2 Warnings
      |ROOT
      |  compiler.plugin
      |===>:app:compile
      |    :lib:compile
    """.trimMargin())
  }

  @Test
  fun testFilterApplyAllNodesFilteredOut() {
    // Arrange
    model.selectNode(model.treeRoot.firstLeaf as TasksTreeNode)
    modelUpdateListenerCallsCount = 0
    modelUpdateListenerCallsWithTreeUpdateCount = 0

    // Act - apply filter
    model.applyFilter(TasksFilter.DEFAULT.copy(showTaskSourceTypes = setOf(PluginSourceType.BUILD_SRC)))

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Tasks determining build duration - Total: 4.0s, Filtered: 0.0s
      |ROOT
    """.trimMargin())
    assertThat(modelUpdateListenerCallsCount).isEqualTo(1)
    assertThat(modelUpdateListenerCallsWithTreeUpdateCount).isEqualTo(1)

    // Act - group by plugin
    model.selectGrouping(Grouping.BY_PLUGIN)

    // Assert
    assertThat(model.print()).isEqualTo("""
      |Plugins with tasks determining build duration - Total: 4.0s, Filtered: 0.0s
      |ROOT
    """.trimMargin())
  }

  private fun TasksDataPageModel.print(): String {
    return treeRoot.preorderEnumeration().asSequence().joinToString(
      prefix = "${treeHeaderText}\n",
      separator = "\n"
    ) {
      if (it is TasksTreeNode) {
        if (selectedNode?.descriptor?.pageId == it.descriptor.pageId) {
          ">".padStart(it.level * 2, padChar = '=') + it.descriptor.pageId.id
        }
        else {
          "".padStart(it.level * 2) + it.descriptor.pageId.id
        }
      }
      else {
        "ROOT"
      }
    }
  }
}