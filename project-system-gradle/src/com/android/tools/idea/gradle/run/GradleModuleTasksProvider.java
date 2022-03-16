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
package com.android.tools.idea.gradle.run;

import static com.android.tools.idea.gradle.project.build.invoker.TestCompileType.UNIT_TESTS;

import com.android.tools.idea.gradle.project.build.invoker.GradleTaskFinder;
import com.android.tools.idea.gradle.project.build.invoker.TestCompileType;
import com.android.tools.idea.gradle.util.BuildMode;
import com.google.common.collect.ListMultimap;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GradleModuleTasksProvider {
  @NotNull private final Project myProject;
  @NotNull private final Module[] myModules;

  GradleModuleTasksProvider(@NotNull Module[] modules) {
    myModules = modules;
    if (myModules.length == 0) {
      throw new IllegalArgumentException("No modules provided");
    }
    myProject = myModules[0].getProject();
  }

  @NotNull
  public ListMultimap<Path, String> getUnitTestTasks(@NotNull BuildMode buildMode) {
    // Make sure all "intermediates/classes" directories are up-to-date.
    Module[] affectedModules = getAffectedModules(myProject, myModules);
    return GradleTaskFinder.getInstance().findTasksToExecuteForTest(affectedModules, myModules, buildMode, UNIT_TESTS);
  }

  @NotNull
  private static Module[] getAffectedModules(@NotNull Project project, @NotNull Module[] modules) {
    final CompilerManager compilerManager = CompilerManager.getInstance(project);
    CompileScope scope = compilerManager.createModulesCompileScope(modules, true, true);
    return scope.getAffectedModules();
  }

  @NotNull
  public ListMultimap<Path, String> getTasksFor(@NotNull BuildMode buildMode, @NotNull TestCompileType testCompileType) {
    return GradleTaskFinder.getInstance().findTasksToExecute(myModules, buildMode, testCompileType);
  }
}
