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
package com.android.tools.idea.testartifacts.instrumented.testsuite.actions

import com.android.flags.junit.RestoreFlagRule
import com.android.tools.idea.flags.StudioFlags
import com.google.common.truth.Truth.assertThat
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.RunsInEdt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
@RunsInEdt
class ImportUtpResultActionTest {
  private val projectRule = ProjectRule()
  private val disposableRule = DisposableRule()
  private val temporaryFolder = TemporaryFolder()

  @get:Rule
  val rules: RuleChain = RuleChain
    .outerRule(projectRule)
    .around(EdtRule())
    .around(disposableRule)
    .around(temporaryFolder)
    .around(RestoreFlagRule(StudioFlags.UTP_TEST_RESULT_SUPPORT))

  private val importUtpResultAction = ImportUtpResultAction()
  private lateinit var utpProtoFile: File

  @Before
  fun setUp() {
    utpProtoFile = temporaryFolder.newFile()
  }

  @Test
  fun importUtpResults() {
    importUtpResultAction.parseResultsAndDisplay(utpProtoFile, disposableRule.disposable, projectRule.project)
    val toolWindow = importUtpResultAction.getToolWindow(projectRule.project)
    assertThat(toolWindow.contentManager.contents).isNotEmpty()
  }
  
  @Test
  fun importUtpResultPreCreateContentManager() {
    RunContentManager.getInstance(projectRule.project)
    val toolWindow = ToolWindowManager.getInstance(projectRule.project)
      .getToolWindow(ImportUtpResultAction.IMPORTED_TEST_WINDOW_ID)
    assertThat(toolWindow).isNull()
    importUtpResultAction.parseResultsAndDisplay(utpProtoFile, disposableRule.disposable, projectRule.project)
    val newToolWindow = importUtpResultAction.getToolWindow(projectRule.project)
    assertThat(newToolWindow.contentManager.contents).isNotEmpty()
  }

  @Test
  fun enableUtpResultSupport() {
    StudioFlags.UTP_TEST_RESULT_SUPPORT.override(true)
    val anActionEvent = AnActionEvent(null, DataContext{ projectRule.project },
                                      ActionPlaces.UNKNOWN, Presentation(),
                                      ActionManager.getInstance(), 0)
    ImportUtpResultAction().update(anActionEvent)
    assertThat(anActionEvent.presentation.isEnabled).isTrue()
  }
}