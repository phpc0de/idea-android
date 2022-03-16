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
package com.android.tools.idea.gradle.project.sync.setup.module;

import com.android.tools.idea.gradle.project.sync.ModuleSetupContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ModuleSetupStep<T> {
  protected boolean myShouldTerminateSetup = false;

  public final void setUpModule(@NotNull ModuleSetupContext context, @Nullable T gradleModel) {
    if (gradleModel == null) {
      return;
    }

    // Reset state
    myShouldTerminateSetup = false;

    doSetUpModule(context, gradleModel);
  }

  protected abstract void doSetUpModule(@NotNull ModuleSetupContext context, @NotNull T gradleModel);

  public boolean invokeOnBuildVariantChange() {
    return false;
  }

  /**
   * @return whether or not to terminate the setup of a module once this step has run.
   */
  public boolean shouldTerminateSetup() { return myShouldTerminateSetup; }
}
