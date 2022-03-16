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
package com.android.tools.profilers;

import com.android.tools.adtui.stdui.ContextMenuItem;
import java.util.ArrayList;
import javax.swing.JComponent;

public class ProfilerContextMenu {
  private static final String PROFILER_CONTEXT_MENU = "ProfilerContextMenu";

  private ArrayList<ContextMenuItem> myMenuItems;

  public ProfilerContextMenu() {
    myMenuItems = new ArrayList<>();
  }

  public void add(ContextMenuItem... items) {
    for (ContextMenuItem item : items) {
      myMenuItems.add(item);
    }
  }

  public ArrayList<ContextMenuItem> getContextMenuItems() {
    return myMenuItems;
  }

  public static ProfilerContextMenu createIfAbsent(JComponent component) {
    ProfilerContextMenu contextMenu = (ProfilerContextMenu)component.getClientProperty(PROFILER_CONTEXT_MENU);
    if (contextMenu == null) {
      contextMenu = new ProfilerContextMenu();
      component.putClientProperty(PROFILER_CONTEXT_MENU, contextMenu);
    }
    return contextMenu;
  }
}
