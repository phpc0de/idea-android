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
package com.android.tools.idea.gradle.project.sync.hyperlink;

import static com.android.tools.idea.gradle.project.AndroidStudioGradleInstallationManager.setJdkAsEmbedded;
import static com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger.TRIGGER_QF_JDK_CHANGED_TO_EMBEDDED;

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.project.hyperlink.NotificationHyperlink;
import com.android.tools.idea.sdk.IdeSdks;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class UseEmbeddedJdkHyperlink extends NotificationHyperlink {
  public UseEmbeddedJdkHyperlink() {
    super("useEmbeddedJdk", "Use Embedded JDK (" + IdeSdks.getInstance().getEmbeddedJdkPath() + ")");
  }

  @Override
  protected void execute(@NotNull Project project) {
    ApplicationManager.getApplication().runWriteAction(() -> setJdkAsEmbedded(project));
    GradleSyncInvoker.getInstance().requestProjectSync(project, TRIGGER_QF_JDK_CHANGED_TO_EMBEDDED);
  }
}
