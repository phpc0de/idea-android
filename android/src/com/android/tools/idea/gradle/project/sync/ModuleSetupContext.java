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
package com.android.tools.idea.gradle.project.sync;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;

public class ModuleSetupContext {
  @NotNull private final Module myModule;
  @NotNull private final IdeModifiableModelsProvider myIdeModelsProvider;

  @VisibleForTesting
  ModuleSetupContext(@NotNull Module module, @NotNull IdeModifiableModelsProvider ideModelsProvider) {
    myModule = module;
    myIdeModelsProvider = ideModelsProvider;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  @NotNull
  public IdeModifiableModelsProvider getIdeModelsProvider() {
    return myIdeModelsProvider;
  }

  @NotNull
  public ModifiableRootModel getModifiableRootModel() {
    return myIdeModelsProvider.getModifiableRootModel(myModule);
  }

  public static class Factory {
    @NotNull
    public ModuleSetupContext create(@NotNull Module module, @NotNull IdeModifiableModelsProvider ideModelsProvider) {
      return new ModuleSetupContext(module, ideModelsProvider);
    }
  }
}
