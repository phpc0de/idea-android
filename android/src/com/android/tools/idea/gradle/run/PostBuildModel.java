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
package com.android.tools.idea.gradle.run;

import com.android.builder.model.AppBundleProjectBuildOutput;
import com.android.builder.model.InstantAppProjectBuildOutput;
import com.android.builder.model.ProjectBuildOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model with all the information needed in post builds.
 * This model is built from the result of {@link OutputBuildAction} and should be used instead of the inner classes of the action.
 */
public class PostBuildModel {
  @NotNull private final OutputBuildAction.PostBuildProjectModels myPostBuildProjectModels;

  public PostBuildModel(@NotNull OutputBuildAction.PostBuildProjectModels outputs) {
    myPostBuildProjectModels = outputs;
  }

  @Nullable
  private <T> T findOutputModel(@Nullable String gradlePath, @NotNull Class<T> modelType) {
    if (gradlePath == null) {
      return null;
    }

    OutputBuildAction.PostBuildModuleModels postBuildModuleModels = myPostBuildProjectModels.getModels(gradlePath);
    return postBuildModuleModels == null ? null : postBuildModuleModels.findModel(modelType);
  }

  @Nullable
  public ProjectBuildOutput findProjectBuildOutput(@Nullable String gradlePath) {
    return findOutputModel(gradlePath, ProjectBuildOutput.class);
  }

  @Nullable
  public InstantAppProjectBuildOutput findInstantAppProjectBuildOutput(@Nullable String gradlePath) {
    return findOutputModel(gradlePath, InstantAppProjectBuildOutput.class);
  }

  @Nullable
  public AppBundleProjectBuildOutput findAppBundleProjectBuildOutput(@Nullable String gradlePath) {
    return findOutputModel(gradlePath, AppBundleProjectBuildOutput.class);
  }
}
