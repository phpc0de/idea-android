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
package com.android.build.attribution.ui.view

import com.android.build.attribution.BuildAttributionWarningsFilter
import com.android.build.attribution.ui.MockUiData
import com.android.build.attribution.ui.model.BuildAnalyzerViewModel
import com.android.tools.adtui.TreeWalker
import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import org.jetbrains.kotlin.utils.keysToMap
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.awt.Dimension

class BuildAnalyzerComboBoxViewTest {
  @get:Rule
  val applicationRule: ApplicationRule = ApplicationRule()

  @get:Rule
  val edtRule = EdtRule()

  val model = BuildAnalyzerViewModel(MockUiData(), BuildAttributionWarningsFilter())
  val mockHandlers = Mockito.mock(ViewActionHandlers::class.java)
  lateinit var view: BuildAnalyzerComboBoxView

  @Before
  fun setUp() {
    view = BuildAnalyzerComboBoxView(model, mockHandlers).apply {
      wholePanel.size = Dimension(600, 200)
    }
  }

  @Test
  @RunsInEdt
  fun testViewCreated() {
    // Assert
    val expectedElementsVisibility = mapOf(
      "build-overview" to true,
      "build-overview-additional-controls" to true,
      "tasks-view" to false,
      "tasks-view-additional-controls" to false,
      "warnings-view" to false,
      "warnings-view-additional-controls" to false
    )
    assertThat(grabElementsVisibilityStatus(expectedElementsVisibility.keys)).isEqualTo(expectedElementsVisibility)
    assertThat(view.dataSetCombo.selectedItem).isEqualTo(BuildAnalyzerViewModel.DataSet.OVERVIEW)
    Mockito.verifyZeroInteractions(mockHandlers)
  }

  @Test
  @RunsInEdt
  fun testViewChangedToTasks() {
    // Act
    model.selectedData = BuildAnalyzerViewModel.DataSet.TASKS

    // Assert
    val expectedElementsVisibility = mapOf(
      "build-overview" to false,
      "build-overview-additional-controls" to false,
      "tasks-view" to true,
      "tasks-view-additional-controls" to true,
      "warnings-view" to false,
      "warnings-view-additional-controls" to false
    )
    assertThat(grabElementsVisibilityStatus(expectedElementsVisibility.keys)).isEqualTo(expectedElementsVisibility)
    Mockito.verifyZeroInteractions(mockHandlers)
    assertThat(view.dataSetCombo.selectedItem).isEqualTo(BuildAnalyzerViewModel.DataSet.TASKS)
  }

  @Test
  @RunsInEdt
  fun testViewChangedToWarnings() {
    // Act
    model.selectedData = BuildAnalyzerViewModel.DataSet.WARNINGS

    // Assert
    val expectedElementsVisibility = mapOf(
      "build-overview" to false,
      "build-overview-additional-controls" to false,
      "tasks-view" to false,
      "tasks-view-additional-controls" to false,
      "warnings-view" to true,
      "warnings-view-additional-controls" to true
    )
    assertThat(grabElementsVisibilityStatus(expectedElementsVisibility.keys)).isEqualTo(expectedElementsVisibility)
    Mockito.verifyZeroInteractions(mockHandlers)
    assertThat(view.dataSetCombo.selectedItem).isEqualTo(BuildAnalyzerViewModel.DataSet.WARNINGS)
  }

  @Test
  @RunsInEdt
  fun testActionHandlerTriggeredOnDataSetChangeToNew() {
    // Pre-requirement
    assertThat(view.dataSetCombo.selectedItem).isEqualTo(BuildAnalyzerViewModel.DataSet.OVERVIEW)

    view.dataSetCombo.selectedItem = BuildAnalyzerViewModel.DataSet.WARNINGS
    Mockito.verify(mockHandlers).dataSetComboBoxSelectionUpdated(BuildAnalyzerViewModel.DataSet.WARNINGS)
    Mockito.verifyNoMoreInteractions(mockHandlers)
  }

  @Test
  @RunsInEdt
  fun testActionHandlerNotTriggeredOnDataSetChangeToAlreadySelected() {
    // Pre-requirement
    assertThat(view.dataSetCombo.selectedItem).isEqualTo(BuildAnalyzerViewModel.DataSet.OVERVIEW)

    view.dataSetCombo.selectedItem = BuildAnalyzerViewModel.DataSet.OVERVIEW
    Mockito.verifyZeroInteractions(mockHandlers)
  }

  private fun grabElementsVisibilityStatus(names: Set<String>): Map<String, Boolean> {
    val descendants = TreeWalker(view.wholePanel).descendants()
    return names.keysToMap { name ->
      descendants.find { it.name == name }?.isVisible == true
    }
  }
}