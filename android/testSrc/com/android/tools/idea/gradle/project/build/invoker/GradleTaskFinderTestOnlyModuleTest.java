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
package com.android.tools.idea.gradle.project.build.invoker;

import static com.android.tools.idea.Projects.getBaseDirPath;
import static com.android.tools.idea.gradle.util.BuildMode.ASSEMBLE;
import static com.android.tools.idea.gradle.util.BuildMode.REBUILD;
import static com.android.tools.idea.testing.TestProjectPaths.TEST_ONLY_MODULE;
import static com.google.common.truth.Truth.assertThat;

import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.google.common.collect.ListMultimap;
import com.intellij.openapi.module.Module;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class GradleTaskFinderTestOnlyModuleTest extends AndroidGradleTestCase {
  public void testAssembleTasksCorrect() throws Exception {
    loadProject(TEST_ONLY_MODULE);
    GradleTaskFinder taskFinder = GradleTaskFinder.getInstance();
    Module[] modules = new Module[]{getModule("test")};
    File projectPath = getBaseDirPath(getProject());
    ListMultimap<Path, String> tasksPerProject = taskFinder.findTasksToExecute(modules, ASSEMBLE, TestCompileType.ALL);
    List<String> tasks = tasksPerProject.get(projectPath.toPath());
    assertThat(tasks).containsExactly(":app:assembleDebug", ":test:assembleDebug");
  }

  public void testAssembleTasksNotDuplicated() throws Exception {
    loadProject(TEST_ONLY_MODULE);
    GradleTaskFinder taskFinder = GradleTaskFinder.getInstance();
    Module[] modules = new Module[]{getModule("test"), getModule("app")};
    File projectPath = getBaseDirPath(getProject());
    ListMultimap<Path, String> tasksPerProject = taskFinder.findTasksToExecute(modules, REBUILD, TestCompileType.ALL);
    List<String> tasks = tasksPerProject.get(projectPath.toPath());
    assertThat(tasks).containsExactly("clean", ":app:assembleDebug", ":test:assembleDebug");
    // Make sure clean is the first task (b/78443416)
    assertThat(tasks.get(0)).isEqualTo("clean");
  }
}
