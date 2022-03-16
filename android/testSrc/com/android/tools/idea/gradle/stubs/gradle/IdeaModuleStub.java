/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.gradle.stubs.gradle;

import com.android.tools.idea.gradle.stubs.FileStructure;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.HierarchicalElement;
import org.gradle.tooling.model.ProjectIdentifier;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.idea.IdeaCompilerOutput;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public class IdeaModuleStub implements IdeaModule {
  @NotNull private final List<IdeaContentRoot> myContentRoots = new ArrayList<>();
  @NotNull private final List<IdeaDependency> myDependencies = new ArrayList<>();

  @NotNull private final String myName;
  @NotNull private final IdeaProjectStub myParent;
  @NotNull private final FileStructure myFileStructure;
  @NotNull private final GradleProjectStub myGradleProject;

  IdeaModuleStub(@NotNull String name, @NotNull IdeaProjectStub parent, @NotNull String...tasks) {
    myName = name;
    myParent = parent;
    myFileStructure = new FileStructure(parent.getRootDir(), name);
    myContentRoots.add(new IdeaContentRootStub(getRootDir()));
    File projectFile = myFileStructure.createProjectFile(GradleConstants.DEFAULT_SCRIPT_NAME);
    myGradleProject = new GradleProjectStub(name, ":" + name, parent.getRootDir(), projectFile, tasks);
  }

  /**
   * @return this module's root directory.
   */
  @NotNull
  public File getRootDir() {
    return myFileStructure.getRootFolderPath();
  }

  @Override
  public String getJdkName() throws UnsupportedMethodException {
    return "Test JDK";
  }

  @NotNull
  @Override
  public DomainObjectSet<? extends IdeaContentRoot> getContentRoots() {
    return ImmutableDomainObjectSet.of(myContentRoots);
  }

  @Override
  public ProjectIdentifier getProjectIdentifier() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public GradleProjectStub getGradleProject() {
    return myGradleProject;
  }

  @NotNull
  @Override
  public IdeaProjectStub getParent() {
    return getProject();
  }

  @Override
  public DomainObjectSet<? extends HierarchicalElement> getChildren() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IdeaProjectStub getProject() {
    return myParent;
  }

  @Override
  @Nullable
  public IdeaCompilerOutput getCompilerOutput() {
    return null;
  }

  public void addDependency(@NotNull IdeaDependency dependency) {
    myDependencies.add(dependency);
  }

  @Override
  public DomainObjectSet<? extends IdeaDependency> getDependencies() {
    return ImmutableDomainObjectSet.of(myDependencies);
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getDescription() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public IdeaJavaLanguageSettings getJavaLanguageSettings() { return null; }
}
