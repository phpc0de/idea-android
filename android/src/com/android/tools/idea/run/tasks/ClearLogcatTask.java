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
package com.android.tools.idea.run.tasks;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.logcat.AndroidLogcatService;
import com.android.tools.idea.logcat.AndroidLogcatToolWindowFactory;
import com.android.tools.idea.logcat.AndroidLogcatView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class ClearLogcatTask implements LaunchTask {
  private final Project myProject;

  private static final String ID = "CLEAR_LOGCAT";

  public ClearLogcatTask(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Clearing logcat";
  }

  @Override
  public int getDuration() {
    return LaunchTaskDurations.ASYNC_TASK;
  }

  @Override
  public LaunchResult run(@NotNull LaunchContext launchContext) {
    clearLogcatAndConsole(myProject, launchContext.getDevice());
    return LaunchResult.success();
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  private static void clearLogcatAndConsole(@NotNull final Project project, @NotNull final IDevice device) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        final ToolWindow toolWindow =
          ToolWindowManager.getInstance(project).getToolWindow(AndroidLogcatToolWindowFactory.getToolWindowId());
        if (toolWindow == null) {
          return;
        }

        for (Content content : toolWindow.getContentManager().getContents()) {
          final AndroidLogcatView view = content.getUserData(AndroidLogcatView.ANDROID_LOGCAT_VIEW_KEY);

          if (view != null) {
            AndroidLogcatService.getInstance().clearLogcat(device, project);
          }
        }
      }
    });
  }
}
