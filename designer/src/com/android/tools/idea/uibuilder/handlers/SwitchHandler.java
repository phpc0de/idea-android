/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.handlers;

import com.android.tools.idea.common.model.NlComponent;
import com.google.common.collect.ImmutableList;
import icons.StudioIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.android.SdkConstants.*;

public class SwitchHandler extends ButtonHandler {
  @Override
  @NotNull
  public List<String> getInspectorProperties() {
    return ImmutableList.of(
      ATTR_STYLE,
      ATTR_SWITCH_TEXT_APPEARANCE,
      ATTR_SWITCH_MIN_WIDTH,
      ATTR_SWITCH_PADDING,
      ATTR_THUMB,
      ATTR_THUMB_TINT,
      ATTR_TRACK,
      ATTR_TRACK_TINT,
      ATTR_TEXT_ON,
      ATTR_TEXT_OFF,
      ATTR_CHECKED,
      TOOLS_NS_NAME_PREFIX + ATTR_CHECKED,
      ATTR_SHOW_TEXT,
      ATTR_SPLIT_TRACK);
  }

  @Override
  @NotNull
  public List<String> getBaseStyles(@NotNull String tagName) {
    return ImmutableList.of(PREFIX_ANDROID + "Widget.CompoundButton." + tagName);
  }

  @NotNull
  @Override
  public Icon getIcon(@NotNull NlComponent component) {
    return StudioIcons.LayoutEditor.Menu.SWITCH;
  }
}
