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
package com.android.tools.idea.gradle.project.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public interface GradleModuleModels extends Serializable {
  /**
   * Obtain the single model with given modelType.
   * Use {@link #findModels(Class)} if there're multiple models with given type.
   */
  @Nullable
  <T> T findModel(@NotNull Class<T> modelType);

  /**
   * Obtain list of models with the given modelType.
   */
  @Nullable
  <T> List<T> findModels(@NotNull Class<T> modelType);

  @NotNull
  String getModuleName();
}
