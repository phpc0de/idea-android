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
package com.android.tools.idea.tests.gui.framework.fixture;

import org.fest.swing.core.Robot;
import com.android.tools.idea.ui.resourcechooser.ColorPicker;
import org.jetbrains.annotations.NotNull;

public class SlideFixture extends JComponentFixture<SlideFixture, ColorPicker.SlideComponent> {
  public SlideFixture(@NotNull Robot robot, @NotNull ColorPicker.SlideComponent target) {
    super(SlideFixture.class, robot, target);
  }
}
