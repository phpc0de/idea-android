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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PsdModuleModels implements GradleModuleModels {
  @NotNull private final String myName;
  @NotNull private final Map<Class, List<Object>> myModelsByType = new HashMap<>();

  public PsdModuleModels(@NotNull String name) {
    myName = name;
  }

  public <T> void addModel(@NotNull Class<T> modelType, @NotNull T model) {
    List<Object> objects = myModelsByType.computeIfAbsent(modelType, k -> new ArrayList<>());
    objects.add(model);
  }

  @Override
  @Nullable
  public <T> T findModel(@NotNull Class<T> modelType) {
    List<Object> models = myModelsByType.get(modelType);
    if (models == null || models.isEmpty()) {
      return null;
    }
    assert models.size() == 1 : "More than one models available, please use findModels() instead.";
    Object model = models.get(0);
    assert modelType.isInstance(model);
    return modelType.cast(model);
  }

  @Override
  @Nullable
  public <T> List<T> findModels(@NotNull Class<T> modelType) {
    List<Object> models = myModelsByType.get(modelType);
    if (models == null || models.isEmpty()) {
      return null;
    }
    return models.stream().map(model -> {
      assert modelType.isInstance(model);
      return modelType.cast(model);
    }).collect(Collectors.toList());
  }

  @Override
  @NotNull
  public String getModuleName() {
    return myName;
  }
}
