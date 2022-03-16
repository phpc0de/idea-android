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
package com.android.tools.idea.ui.resourcemanager.actions

import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.loadNewFile
import com.android.tools.idea.ui.resourcemanager.ResourceExplorer
import com.android.tools.idea.ui.resourcemanager.explorer.ResourceExplorerView
import com.android.tools.idea.ui.resourcemanager.getTestDataDirectory
import com.android.tools.idea.ui.resourcemanager.model.ResourceAssetSet
import com.android.tools.idea.ui.resourcemanager.waitAndAssert
import com.android.tools.idea.ui.resourcemanager.widget.SectionList
import com.android.tools.idea.util.androidFacet
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ShowFileInResourceManagerActionTest {

  @get:Rule
  val rule = AndroidProjectRule.onDisk()

  @Before
  fun setUp() {
    rule.fixture.testDataPath = getTestDataDirectory()
    rule.fixture.copyFileToProject(FN_ANDROID_MANIFEST_XML, FN_ANDROID_MANIFEST_XML)
  }

  private fun findResourceManagerAction(): ShowFileInResourceManagerAction =
    ActionManager.getInstance().getAction("ResourceExplorer.show") as ShowFileInResourceManagerAction

  @Test
  fun actionIsAvailableOnDrawable() {
    val newFile = rule.fixture.loadNewFile("res/drawable-hdpi/icon.xml", "<drawable></drawable>")
    val resourceManagerAction = findResourceManagerAction()
    val testActionEvent = checkActionWithFile(resourceManagerAction, newFile.virtualFile)
    assertTrue { testActionEvent.presentation.isEnabledAndVisible }
    assertEquals(resourceManagerAction.templatePresentation.text, testActionEvent.presentation.text)
  }

  @Test
  fun actionIsNotAvailableOnString() {
    val newFile = rule.fixture.loadNewFile("res/values/strings.xml", "<resource></resource")
    val testActionEvent = checkActionWithFile(findResourceManagerAction(), newFile.virtualFile)
    assertFalse { testActionEvent.presentation.isEnabledAndVisible }
  }

  @Test
  fun itemIsSelectedInResourceManager() {
    val newFile = rule.fixture.loadNewFile("res/drawable-hdpi/icon.xml", "<drawable></drawable>")
    val resourceExplorer = runInEdtAndGet{ ResourceExplorer.createForToolWindow(rule.module.androidFacet!!) }
    val resourceExplorerView = UIUtil.findComponentOfType(resourceExplorer, ResourceExplorerView::class.java)!!
    Disposer.register(rule.project, resourceExplorer)

    waitAndAssert<SectionList>(resourceExplorerView) {
      it != null  && (it.getLists().getOrNull(0)?.model?.size ?: 0) > 0
    }

    val component = UIUtil.findComponentsOfType(resourceExplorer, SectionList::class.java)[0]
    resourceExplorer.selectAsset(rule.module.androidFacet!!, newFile.virtualFile)
    val designAsset = component.selectedValue as ResourceAssetSet
    assertTrue { designAsset.name == "icon" }
  }

  private fun checkActionWithFile(resourceManagerAction: ShowFileInResourceManagerAction,
                                  virtualFile: VirtualFile?): TestActionEvent {
    val dataContext = createDataContext(virtualFile)
    val testActionEvent = TestActionEvent(dataContext, resourceManagerAction)
    runInEdtAndWait {
      resourceManagerAction.update(testActionEvent)
    }
    return testActionEvent
  }

  private fun createDataContext(virtualFile: VirtualFile?): (String) -> Any? = {
    when {
      CommonDataKeys.VIRTUAL_FILE.`is`(it) -> virtualFile
      CommonDataKeys.PROJECT.`is`(it) -> rule.project
      else -> null
    }
  }
}