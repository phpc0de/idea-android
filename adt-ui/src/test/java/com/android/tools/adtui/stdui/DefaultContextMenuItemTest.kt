/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.adtui.stdui

import com.google.common.truth.Truth.assertThat
import com.intellij.icons.AllIcons
import org.junit.Test
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class DefaultContextMenuItemTest {

  @Test
  fun actionRunnableIsCalledOnRun() {
    var runnableCalled = false
    val profilerAction = DefaultContextMenuItem.Builder("").setActionRunnable { runnableCalled = true }.build()

    assertThat(runnableCalled).isFalse()
    profilerAction.run()
    assertThat(runnableCalled).isTrue()
  }

  @Test
  fun actionEnableStatusCanBeChangedDynamically() {
    var shouldEnableAction = false
    val profilerAction = DefaultContextMenuItem.Builder("").setEnableBooleanSupplier { shouldEnableAction }.build()

    assertThat(profilerAction.isEnabled).isFalse()
    shouldEnableAction = true
    assertThat(profilerAction.isEnabled).isTrue()
  }

  @Test
  fun textCanBeEitherStaticOrChangedDynamically() {
    // Passing a supplier to the constructor should allow the text to be dynamically changed.
    var text = "textOne"
    var profilerAction = DefaultContextMenuItem.Builder { text }.build()
    assertThat(profilerAction.text).isEqualTo("textOne")
    text = "textTwo"
    assertThat(profilerAction.text).isEqualTo("textTwo")

    // Passing a string to the constructor creates an action with fixed text.
    text = "textOne"
    profilerAction = DefaultContextMenuItem.Builder(text).build()
    assertThat(profilerAction.text).isEqualTo("textOne")
    text = "textTwo"
    assertThat(profilerAction.text).isEqualTo("textOne")
  }

  @Test
  fun iconCanBeEitherStaticOrChangedDynamically() {
    // Passing a supplier to the setter should allow the icon to be dynamically changed.
    var icon = AllIcons.General.Add
    var profilerAction = DefaultContextMenuItem.Builder("").setIcon { icon }.build()
    assertThat(profilerAction.icon).isEqualTo(AllIcons.General.Add)
    icon = AllIcons.Actions.Back
    assertThat(profilerAction.icon).isEqualTo(AllIcons.Actions.Back)

    // Passing an icon to the setter creates an action with fixed icon.
    icon = AllIcons.General.Add
    profilerAction = DefaultContextMenuItem.Builder("").setIcon(icon).build()
    assertThat(profilerAction.icon).isEqualTo(AllIcons.General.Add)
    icon = AllIcons.Actions.Back
    assertThat(profilerAction.icon).isEqualTo(AllIcons.General.Add)
  }

  @Test
  fun keyStrokesArrayAlwaysExist() {
    // Do not set keystrokes for the action
    var profilerAction = DefaultContextMenuItem.Builder("").build()
    // Make sure we create an empty array of keystrokes anyway
    assertThat(profilerAction.keyStrokes).isNotNull()
    assertThat(profilerAction.keyStrokes).isEmpty()

    // Set a couple of keystrokes for the action
    val a = KeyStroke.getKeyStroke('a')
    val b = KeyStroke.getKeyStroke('b')
    profilerAction = DefaultContextMenuItem.Builder("").setKeyStrokes(a, b).build()
    assertThat(profilerAction.keyStrokes).hasLength(2)
    assertThat(profilerAction.keyStrokes[0]).isEqualTo(a)
    assertThat(profilerAction.keyStrokes[1]).isEqualTo(b)
  }

  @Test
  fun tooltipIsComposedByTextAndFirstKeystroke() {
    // Create a profiler action with no keystrokes
    var profilerAction = DefaultContextMenuItem.Builder("hi").build()
    // Tooltip should contain only the text
    assertThat(profilerAction.defaultToolTipText).isEqualTo("hi")

    // Create a profiler action with text defined and a couple of keystrokes
    val a = KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)
    val b = KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)
    profilerAction = DefaultContextMenuItem.Builder("hello").setKeyStrokes(a, b).build()
    // Check the tooltip uses the text and only the first keystroke
    assertThat(profilerAction.defaultToolTipText).isEqualTo("hello (A)")
  }
}