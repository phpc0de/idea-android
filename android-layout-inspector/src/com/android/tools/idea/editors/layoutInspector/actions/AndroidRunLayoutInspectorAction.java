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
package com.android.tools.idea.editors.layoutInspector.actions;

import com.android.ddmlib.Client;
import com.android.tools.idea.editors.layoutInspector.AndroidLayoutInspectorService;
import com.android.tools.idea.ui.LayoutInspectorSettingsKt;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import icons.StudioIcons;
import org.jetbrains.android.actions.AndroidProcessChooserDialog;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AndroidRunLayoutInspectorAction extends AnAction {
  public AndroidRunLayoutInspectorAction() {
    super(AndroidBundle.messagePointer("android.ddms.actions.layoutinspector.title.legacy"),
          AndroidBundle.messagePointer("android.ddms.actions.layoutinspector.description"),
          StudioIcons.Shell.Menu.LAYOUT_INSPECTOR);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(!LayoutInspectorSettingsKt.getEnableLiveLayoutInspector());
    if (isDebuggerPaused(e.getProject())) {
      e.getPresentation().setDescription(AndroidBundle.messagePointer("android.ddms.actions.layoutinspector.description.disabled"));
      e.getPresentation().setEnabled(false);
    }
    else {
      e.getPresentation().setDescription(AndroidBundle.messagePointer("android.ddms.actions.layoutinspector.description"));
      e.getPresentation().setEnabled(true);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    assert project != null;

    AndroidProcessChooserDialog dialog = new AndroidProcessChooserDialog(project, false);
    dialog.show();
    if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
      Client client = dialog.getClient();
      if (client != null) {
        project.getService(AndroidLayoutInspectorService.class).getTask(project, client).queue();
      }
      else {
        Logger.getInstance(AndroidRunLayoutInspectorAction.class).warn("Not launching layout inspector - no client selected");
      }
    }
  }

  public static boolean isDebuggerPaused(@Nullable Project project) {
    if (project == null) {
      return false;
    }

    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    return session != null && !session.isStopped() && session.isPaused();
  }
}
