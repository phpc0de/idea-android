/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework.driver;

import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBTextField;
import org.fest.swing.annotation.RunsInEDT;
import org.fest.swing.core.Robot;
import org.fest.swing.driver.JComponentDriver;
import org.fest.swing.driver.TextDisplayDriver;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;

public class SearchTextFieldDriver extends JComponentDriver<SearchTextField> implements TextDisplayDriver<SearchTextField> {
  public SearchTextFieldDriver(@NotNull Robot robot) {
    super(robot);
  }

  @Override
  @RunsInEDT
  public void requireText(@NotNull SearchTextField component, String expected) {
    assertThat(textOf(component)).isEqualTo(expected);
  }

  @Override
  @RunsInEDT
  public void requireText(@NotNull SearchTextField component, final @NotNull Pattern pattern) {
    assertThat(textOf(component)).matches(pattern.pattern());
  }

  @Override
  @RunsInEDT
  @Nullable
  public String textOf(final @NotNull SearchTextField component) {
    return GuiQuery.get(component::getText);
  }

  @RunsInEDT
  public void enterText(@NotNull SearchTextField textBox, @NotNull String text) {
    JBTextField textField = textBox.getTextEditor();
    focusAndWaitForFocusGain(textField);
    robot.enterText(text);
  }

  @RunsInEDT
  public void deleteText(SearchTextField textBox) {
    focusAndWaitForFocusGain(textBox.getTextEditor());
    GuiTask.execute(() -> textBox.getTextEditor().selectAll());
    robot.pressAndReleaseKey(KeyEvent.VK_DELETE);
  }

  @Override
  @RunsInEDT
  public void pressAndReleaseKeys(@NotNull SearchTextField textBox, @NotNull int... keyCodes) {
    focusAndWaitForFocusGain(textBox.getTextEditor());
    robot.pressAndReleaseKeys(keyCodes);
  }
}
