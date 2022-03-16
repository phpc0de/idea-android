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
package com.android.tools.adtui.imagediff

import com.android.tools.adtui.stdui.CommonTabbedPane
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private class CommonTabbedPaneEntriesRegistrar : ImageDiffEntriesRegistrar() {
  init {
    register(CommonTabbedPaneEntry("common_tabbed_pane_top.png", SwingConstants.TOP))
    register(CommonTabbedPaneEntry("common_tabbed_pane_left.png", SwingConstants.LEFT))
    register(CommonTabbedPaneEntry("common_tabbed_pane_bottom.png", SwingConstants.BOTTOM))
    register(CommonTabbedPaneEntry("common_tabbed_pane_right.png", SwingConstants.RIGHT))
  }

  private class CommonTabbedPaneEntry(fileName: String, tabPlacement: Int) : ImageDiffEntry(fileName) {
    private val panel = JPanel(BorderLayout())

    init {
      val tabbedPane = CommonTabbedPane()
      tabbedPane.font = ImageDiffTestUtil.getDefaultFont()
      tabbedPane.tabPlacement = tabPlacement
      tabbedPane.addTab("tab1", JLabel())
      tabbedPane.addTab("tab2", JLabel())
      tabbedPane.tabPlacement = tabPlacement
      panel.add(tabbedPane, BorderLayout.CENTER)
      panel.setSize(200, 200)
    }

    override fun generateComponentImage(): BufferedImage {
      return ImageDiffTestUtil.getImageFromComponent(panel)
    }
  }
}
