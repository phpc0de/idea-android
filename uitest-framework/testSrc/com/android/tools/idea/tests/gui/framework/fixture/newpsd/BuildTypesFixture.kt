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
package com.android.tools.idea.tests.gui.framework.fixture.newpsd

import com.android.tools.idea.tests.gui.framework.clickToolButton
import com.android.tools.idea.tests.gui.framework.waitForIdle
import org.fest.swing.core.Robot
import java.awt.Container

open class BuildTypesFixture constructor(
  val robot: Robot,
  val container: Container
) : ConfigPanelFixture() {

  override fun target(): Container = container
  override fun robot(): Robot = robot

  fun clickAdd(): InputNameDialogFixture {
    clickToolButton("Add Build Type")
    return InputNameDialogFixture.find(robot, "Create New Build Type") {
      Thread.sleep(500) // MasterDetailsComponent has up to 500ms delay before acting on selection change.
      waitForIdle()
    }
  }

  fun debuggable(): PropertyEditorFixture = findEditor("Debuggable")
  fun versionNameSuffix(): PropertyEditorFixture = findEditor("Version Name Suffix")
}