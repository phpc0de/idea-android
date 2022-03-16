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
package com.android.tools.idea.tests.gui.framework.fixture;

import com.intellij.ui.EditorTextField;
import org.fest.swing.core.Robot;
import org.fest.swing.fixture.AbstractJComponentFixture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class EditorTextFieldFixture extends AbstractJComponentFixture<EditorTextFieldFixture, EditorTextField, EditorTextFieldDriver> {

  private EditorTextFieldFixture(@NotNull Robot robot, @NotNull EditorTextField target) {
    super(EditorTextFieldFixture.class, robot, target);
  }

  @NotNull
  public static EditorTextFieldFixture find(@NotNull Robot robot, @NotNull Container target) {
    return new EditorTextFieldFixture(robot, robot.finder().findByType(target, EditorTextField.class));
  }

  @NotNull
  public static EditorTextFieldFixture findByLabel(@NotNull Robot robot, @NotNull Container target, @NotNull String label) {
    return new EditorTextFieldFixture(robot, robot.finder().findByLabel(target, label, EditorTextField.class));
  }

  @NotNull
  public static EditorTextFieldFixture findByName(@NotNull Robot robot, @NotNull Container target, @NotNull String name) {
    return new EditorTextFieldFixture(robot, robot.finder().findByName(target, name, EditorTextField.class));
  }

  @NotNull
  @Override
  protected EditorTextFieldDriver createDriver(@NotNull Robot robot) {
    return new EditorTextFieldDriver(robot);
  }

  @NotNull
  public EditorTextFieldFixture requireText(@Nullable String expected) {
    driver().requireText(target(), expected);
    return this;
  }

  @NotNull
  public EditorTextFieldFixture enterText(@NotNull String text) {
    driver().enterText(target(), text);
    return this;
  }

  @NotNull
  public EditorTextFieldFixture replaceText(@NotNull String text) {
    driver().replaceText(target(), text);
    return this;
  }

  @NotNull
  public String getText() {
    return driver().textOf(target());
  }
}
