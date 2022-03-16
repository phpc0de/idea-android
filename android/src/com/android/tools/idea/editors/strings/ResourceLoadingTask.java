/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.editors.strings;

import com.android.tools.idea.editors.strings.table.StringResourceTableModel;
import com.android.tools.idea.res.LocalResourceRepository;
import com.android.tools.idea.res.ResourceRepositoryManager;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ResourceLoadingTask extends Task.Backgroundable {
  @NotNull
  private final StringResourceViewPanel myPanel;

  @NotNull
  private final Supplier<? extends LocalResourceRepository> myGetModuleResources;

  @Nullable
  private StringResourceRepository myRepository;

  ResourceLoadingTask(@NotNull StringResourceViewPanel panel) {
    this(panel, () -> ResourceRepositoryManager.getModuleResources(panel.getFacet()));
  }

  @VisibleForTesting
  ResourceLoadingTask(@NotNull StringResourceViewPanel panel, @NotNull Supplier<? extends LocalResourceRepository> getModuleResources) {
    super(panel.getFacet().getModule().getProject(), "Loading String Resources...");

    myPanel = panel;
    myGetModuleResources = getModuleResources;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    indicator.setIndeterminate(true);
    myRepository = StringResourceRepository.create(myGetModuleResources.get());
  }

  @Override
  public void onSuccess() {
    assert myRepository != null;
    myPanel.getTable().setModel(new StringResourceTableModel(myRepository, myPanel.getFacet().getModule().getProject()));

    myPanel.getLoadingPanel().stopLoading();
  }

  @Override
  public void onCancel() {
    myPanel.getLoadingPanel().stopLoading();
  }
}
