/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.gradle.adtimport;

import static com.android.tools.idea.gradle.adtimport.GradleImport.isAdtProjectDir;
import static com.android.tools.idea.gradle.util.GradleUtil.getDefaultPhysicalPathFromGradlePath;
import static com.android.utils.BuildScriptUtil.findGradleBuildFile;
import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;

import com.android.tools.idea.gradle.adtimport.actions.AndroidImportProjectAction;
import com.android.tools.idea.gradle.project.AndroidModuleImporter;
import com.android.tools.idea.gradle.project.ModuleImporter;
import com.android.tools.idea.gradle.project.ModuleToImport;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Creates new project modules from existing Android Eclipse projects.
 */
public final class AdtModuleImporter extends ModuleImporter {
  @NotNull private final WizardContext myContext;
  @NotNull private final AdtImportProvider myProvider;

  private List<ModuleWizardStep> myWizardSteps;

  private AdtModuleImporter(@NotNull WizardContext context) {
    super();
    myContext = context;
    myProvider = new AdtImportProvider(false);
    myContext.setProjectBuilder(myProvider.getBuilder());
  }

  public static boolean isAdtProjectLocation(@NotNull VirtualFile importSource) {
    VirtualFile target = AndroidImportProjectAction.findImportTarget(importSource);
    VirtualFile targetDir = target.isDirectory() ? target : target.getParent();
    File targetDirFile = virtualToIoFile(targetDir);
    return isAdtProjectDir(targetDirFile) && !findGradleBuildFile(targetDirFile).isFile();
  }

  @Override
  @NotNull
  public List<? extends ModuleWizardStep> createWizardSteps() {
    ModuleWizardStep[] adtImportSteps = myProvider.createSteps(myContext);
    myWizardSteps = Lists.newArrayList(adtImportSteps);
    return myWizardSteps;
  }

  @Override
  public void importProjects(Map<String, VirtualFile> projects) {
    Project project = myContext.getProject();
    assert project != null;
    AdtImportBuilder builder = AdtImportBuilder.getBuilder(myContext);
    assert builder != null;
    GradleImport importer = getGradleImport();
    ImmutableMap.Builder<File, String> modules = ImmutableMap.builder();
    for (Map.Entry<String, VirtualFile> entry : projects.entrySet()) {
      modules.put(virtualToIoFile(entry.getValue()), getDefaultPhysicalPathFromGradlePath(entry.getKey()));
    }

    importer.setImportModuleNames(modules.build());
    if (builder.validate(null, project)) {
      builder.commit(project, null, ModulesProvider.EMPTY_MODULES_PROVIDER, null);
      builder.cleanup();
    }
  }

  @NotNull
  private GradleImport getGradleImport() {
    AdtImportBuilder builder = AdtImportBuilder.getBuilder(myContext);
    assert builder != null;
    GradleImport importer = builder.getImporter();
    assert importer != null;
    return importer;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public boolean canImport(@NotNull VirtualFile importSource) {
    return isAdtProjectLocation(importSource);
  }

  @Override
  @NotNull
  public Set<ModuleToImport> findModules(@NotNull VirtualFile importSource) throws IOException {
    final AdtImportBuilder builder = (AdtImportBuilder)myContext.getProjectBuilder();
    assert builder != null;
    builder.setSelectedProject(virtualToIoFile(importSource));
    final GradleImport gradleImport = getGradleImport();
    gradleImport.importProjects(Collections.singletonList(virtualToIoFile(importSource)));
    Map<String, File> adtProjects = gradleImport.getDetectedModuleLocations();
    Set<ModuleToImport> modules = new HashSet<ModuleToImport>();
    for (final Map.Entry<String, File> entry : adtProjects.entrySet()) {
      VirtualFile location = findFileByIoFile(entry.getValue(), false);
      modules.add(new ModuleToImport(entry.getKey(), location, new Supplier<Iterable<String>>() {
        @Override
        public Iterable<String> get() {
          return gradleImport.getProjectDependencies(entry.getKey());
        }
      }));
    }
    return modules;
  }

  @Override
  public boolean isStepVisible(@NotNull ModuleWizardStep step) {
    return myWizardSteps.contains(step);
  }

  public static class AdtAndroidModuleImporter implements AndroidModuleImporter {
    @NotNull
    @Override
    public ModuleImporter create(@NotNull WizardContext context) {
      return new AdtModuleImporter(context);
    }
  }
}
