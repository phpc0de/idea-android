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
package com.android.tools.idea.testartifacts.scopes;

import com.android.tools.idea.gradle.project.sync.setup.module.dependency.DependencySet;
import com.android.tools.idea.gradle.project.sync.setup.module.dependency.ModuleDependency;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ExcludedModules implements Iterable<Module> {
  @NotNull private final Project myProject;
  @NotNull private final Set<Module> myExcludedModules = new HashSet<>();

  ExcludedModules(@NotNull Module module) {
    myProject = module.getProject();
  }

  void add(@NotNull DependencySet dependencies) {
    for (ModuleDependency dependency : dependencies.onModules()) {
      Module dependencyModule = dependency.getModule();
      if (dependencyModule != null) {
        myExcludedModules.add(dependencyModule);
      }
    }
  }

  void remove(@NotNull DependencySet dependencies) {
    for (ModuleDependency dependency : dependencies.onModules()) {
      Module dependencyModule = dependency.getModule();
      if (dependencyModule != null) {
        myExcludedModules.remove(dependencyModule);
      }
    }
  }

  boolean contains(@Nullable Module module) {
    return myExcludedModules.contains(module);
  }

  @NotNull
  Project getProject() {
    return myProject;
  }

  @Override
  public Iterator<Module> iterator() {
    return myExcludedModules.iterator();
  }
}
