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
package com.android.tools.idea.gradle.project.sync.setup.module.idea;

import com.android.tools.idea.gradle.project.model.JavaModuleModel;
import com.android.tools.idea.gradle.project.sync.setup.module.common.BaseSetup;
import com.android.tools.idea.gradle.project.sync.setup.module.idea.java.ArtifactsByConfigurationModuleSetupStep;
import com.android.tools.idea.gradle.project.sync.setup.module.idea.java.CompilerOutputModuleSetupStep;
import com.android.tools.idea.gradle.project.sync.setup.module.idea.java.JavaFacetModuleSetupStep;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

public class JavaModuleSetup extends BaseSetup<JavaModuleSetupStep, JavaModuleModel> {
  public JavaModuleSetup() {
    this(new JavaFacetModuleSetupStep(), new ArtifactsByConfigurationModuleSetupStep(), new CompilerOutputModuleSetupStep());
  }

  @VisibleForTesting
  JavaModuleSetup(@NotNull JavaModuleSetupStep... setupSteps) {
    super(setupSteps);
  }
}
