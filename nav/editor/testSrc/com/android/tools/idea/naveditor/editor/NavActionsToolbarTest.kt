/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.naveditor.editor

import com.android.tools.idea.common.editor.ActionsToolbar
import com.android.tools.idea.common.error.IssueModel
import com.android.tools.idea.common.error.IssuePanel
import com.android.tools.idea.common.model.NlComponent
import com.android.tools.idea.naveditor.NavModelBuilderUtil.navigation
import com.android.tools.idea.naveditor.NavTestCase
import com.intellij.openapi.actionSystem.DefaultActionGroup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.util.Collections

class NavActionsToolbarTest : NavTestCase() {

  fun testAddActions() {
    val model = model("nav.xml") {
      navigation("root") {
        fragment("f1") {
          action("a1", "f2")
        }
        fragment("f2")
        activity("a1")
      }
    }

    val actionManager = mock(NavActionManager::class.java)
    val surface = model.surface

    surface.selectionModel.clear()
    val issuePanel = mock(IssuePanel::class.java)
    `when`(surface.issuePanel).thenReturn(issuePanel)
    val issueModel = mock(IssueModel::class.java)
    `when`(surface.issueModel).thenReturn(issueModel)
    `when`(surface.actionManager).thenReturn(actionManager)
    `when`(actionManager.getPopupMenuActions(any())).thenReturn(DefaultActionGroup())
    // We use any ?: Collections.emptyList() below because any() returns null and Kotlin will
    // complain during the null checking
    `when`(actionManager.getToolbarActions(Mockito.any() ?: Collections.emptyList())).thenReturn(DefaultActionGroup())
    ActionsToolbar(project, surface)

    val components = listOf(model.find("root")!!)

    verify(actionManager).getToolbarActions(eq(components))

    val f1 = listOf(model.find("f1")!!)
    surface.selectionModel.setSelection(f1)

    verify(actionManager).getToolbarActions(eq(f1))

    val f1AndRoot = listOf(model.find("f1")!!, model.components[0])
    surface.selectionModel.setSelection(f1AndRoot)

    verify(actionManager).getToolbarActions(eq(f1AndRoot))
  }

  fun <T> any(): T = ArgumentMatchers.any() as T
  fun <T> eq(v: T): T = ArgumentMatchers.eq(v) as T
}