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
package com.android.tools.idea.gradle.project.build;

import com.android.tools.idea.gradle.util.BuildMode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BuildContext {
  @NotNull private final Project myProject;
  @NotNull private final List<String> myGradleTasks;
  @Nullable private final BuildMode myBuildMode;

  public BuildContext(@NotNull Project project, @NotNull List<String> gradleTasks, @Nullable BuildMode buildMode) {
    myProject = project;
    myGradleTasks = new ArrayList<>(gradleTasks);
    myBuildMode = buildMode;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public List<String> getGradleTasks() {
    return myGradleTasks;
  }

  @Nullable
  public BuildMode getBuildMode() {
    return myBuildMode;
  }
}
