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
package com.android.tools.idea.run.tasks;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.gradle.util.DynamicAppUtils;
import com.android.tools.idea.run.ApkInfo;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@see SplitApkDeployTaskContext} when deploying an application
 * with multiple Dynamic Feature modules.
 */
public class DynamicAppDeployTaskContext implements SplitApkDeployTaskContext {
  private ApkInfo myApkInfo;
  private List<String> myDisabledFeatures;

  public DynamicAppDeployTaskContext(@NotNull ApkInfo apkInfo, @NotNull List<String> disabledFeatures) {
    myApkInfo = apkInfo;
    myDisabledFeatures = disabledFeatures;
  }

  @NotNull
  @Override
  public String getApplicationId() {
    return myApkInfo.getApplicationId();
  }

  @Override
  public boolean isPatchBuild() {
    // Incremental patching is not yet supported for dynamic apps
    return false;
  }

  @NotNull
  @Override
  public List<File> getArtifacts() {
    return myApkInfo.getFiles().stream()
      .filter(entry -> DynamicAppUtils.isFeatureEnabled(myDisabledFeatures, entry))
      .map(entry -> entry.getApkFile())
      .collect(Collectors.toList());
  }

  @NotNull
  public List<String> getDisabledFeatures() {
    return myDisabledFeatures;
  }

  @Override
  public void notifyInstall(@NotNull Project project, @NotNull IDevice device, boolean status) {
    // Nothing to do, this is a regular deployment
  }
}
