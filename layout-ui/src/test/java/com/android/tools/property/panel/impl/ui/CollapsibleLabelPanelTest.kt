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
package com.android.tools.property.panel.impl.ui

import com.android.tools.adtui.swing.FakeUi
import com.android.tools.adtui.workbench.PropertiesComponentMock
import com.android.tools.property.panel.impl.model.CollapsibleLabelModel
import com.google.common.truth.Truth.assertThat
import com.intellij.util.ui.UIUtil
import org.junit.Test
import java.awt.Dimension
import java.awt.Font

class CollapsibleLabelPanelTest {

  @Test
  fun testDoubleClick() {
    val model = CollapsibleLabelModel("Label", null, false, PropertiesComponentMock())
    val panel = CollapsibleLabelPanel(model, UIUtil.FontSize.NORMAL, Font.BOLD)
    val label = panel.label
    panel.size = Dimension(500, 200)
    panel.doLayout()
    val ui = FakeUi(label)
    ui.mouse.doubleClick(400, 100)
    assertThat(model.expanded).isFalse()
    ui.mouse.doubleClick(400, 100)
    assertThat(model.expanded).isTrue()
  }
}
