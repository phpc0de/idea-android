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
package com.android.tools.idea.assistant;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import icons.StudioIcons;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers the creation of the Developer Services side panel.
 */
public class OpenAssistSidePanelAction extends AnAction {

  public static final String TOOL_WINDOW_TITLE = "Assistant";

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project thisProject = event.getProject();
    final String actionId = ActionManager.getInstance().getId(this);

    assert thisProject != null;
    openWindow(actionId, thisProject);
  }

  /**
   * Opens the assistant associated with the given actionId at the end of event thread
   */
  public final void openWindow(@NotNull String actionId, @NotNull Project project) {
    ApplicationManager.getApplication().invokeLater(() -> openWindowNow(actionId, project));
  }

  /**
   * Opens the assistant associated with the given actionId
   */
  private void openWindowNow(@NotNull String actionId, @NotNull Project project) {
    AssistToolWindowFactory factory = new AssistToolWindowFactory(actionId);
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_TITLE);

    if (toolWindow == null) {
      // NOTE: canWorkInDumbMode must be true or the window will close on gradle sync.
      toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_TITLE, false, ToolWindowAnchor.RIGHT, project, true);
    }
    toolWindow.setHelpId(actionId);
    toolWindow.setIcon(StudioIcons.Shell.ToolWindows.ASSISTANT);

    factory.createToolWindowContent(project, toolWindow, true);

    // Always active the window, in case it was previously minimized.
    toolWindow.activate(null);
  }
}
