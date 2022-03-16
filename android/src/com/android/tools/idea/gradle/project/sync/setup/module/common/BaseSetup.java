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
package com.android.tools.idea.gradle.project.sync.setup.module.common;

import com.android.tools.idea.gradle.project.sync.ModuleSetupContext;
import com.android.tools.idea.gradle.project.sync.setup.module.ModuleSetupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseSetup<T extends ModuleSetupStep<M>, M> {
  protected final T[] mySetupSteps;

  protected BaseSetup(T... steps) {
    mySetupSteps = steps;
  }

  protected void beforeSetup(@NotNull ModuleSetupContext context, @Nullable M model) { }

  public void setUpModule(@NotNull ModuleSetupContext context, @Nullable M model) {
    beforeSetup(context, model);

    for (T step : mySetupSteps) {
      if (shouldRunSyncStep(step)) {
        step.setUpModule(context, model);
        if (step.shouldTerminateSetup()) {
          return;
        }
      }
    }
  }

  protected boolean shouldRunSyncStep(T step) {
    return true;
  }
}
