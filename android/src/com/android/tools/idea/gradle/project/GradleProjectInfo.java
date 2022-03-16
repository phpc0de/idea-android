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
package com.android.tools.idea.gradle.project;

import static com.android.tools.idea.gradle.util.GradleUtil.findGradleBuildFile;
import static com.android.tools.idea.gradle.util.GradleUtil.findGradleSettingsFile;
import static com.intellij.openapi.actionSystem.LangDataKeys.MODULE_CONTEXT_ARRAY;
import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.MODULE;
import static com.intellij.openapi.util.io.FileUtil.filesEqual;
import static com.intellij.util.containers.ContainerUtil.newConcurrentSet;
import static org.jetbrains.android.facet.AndroidRootUtil.findModuleRootFolderPath;

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.android.tools.idea.model.AndroidModel;
import com.android.tools.idea.project.AndroidProjectInfo;
import com.google.common.collect.ImmutableList;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

public class GradleProjectInfo {
  @NotNull private final Project myProject;
  @NotNull private final ProjectFileIndex myProjectFileIndex;

  private volatile boolean myNewProject;
  private volatile boolean myImportedProject;
  private final ProjectFacetManager myFacetManager;
  private volatile boolean mySkipStartupActivity;

  @NotNull
  public static GradleProjectInfo getInstance(@NotNull Project project) {
    return project.getService(GradleProjectInfo.class);
  }

  public GradleProjectInfo(@NotNull Project project) {
    myProject = project;
    myProjectFileIndex = ProjectFileIndex.getInstance(myProject);
    myFacetManager = ProjectFacetManager.getInstance(myProject);
  }

  public boolean isNewProject() {
    return myNewProject;
  }

  public void setNewProject(boolean newProject) {
    myNewProject = newProject;
  }

  public boolean isSkipStartupActivity() {
    return mySkipStartupActivity;
  }

  public void setSkipStartupActivity(boolean skipStartupActivity) {
    mySkipStartupActivity = skipStartupActivity;
  }

  public boolean isImportedProject() {
    return myImportedProject;
  }

  public void setImportedProject(boolean importedProject) {
    myImportedProject = importedProject;
  }

  /**
   * Indicates whether Gradle is used to build at least one module in this project.
   * Note: {@link AndroidProjectInfo#requiresAndroidModel())} indicates whether a project requires an {@link AndroidModel}.
   * That method should be preferred in almost all cases. Use this method only if you explicitly need to check whether the model is
   * Gradle-specific.
   */
  public boolean isBuildWithGradle() {
    return ReadAction.compute(() -> {
      if (myProject.isDisposed()) {
        return false;
      }
      if (isBeingInitializedAsGradleProject(myProject)) {
        return true;
      }
      if (myFacetManager.hasFacets(GradleFacet.getFacetTypeId())) {
        return true;
      }
      // See https://code.google.com/p/android/issues/detail?id=203384
      // This could be a project without modules. Check that at least it synced with Gradle.
      if (GradleSyncState.getInstance(myProject).getLastSyncFinishedTimeStamp() != -1L) {
        return true;
      }
      if (!GradleSettings.getInstance(myProject).getLinkedProjectsSettings().isEmpty()){
        return true;
      }
      return hasTopLevelGradleFile();
    });
  }

  /**
   * Indicates whether the project has Gradle facets.
   * e.g. this is used to check if an opened Gradle project has already been imported or not yet.
   */
  public boolean hasGradleFacets() {
    return myFacetManager.hasFacets(GradleFacet.getFacetTypeId());
  }

  /**
   * Indicates whether the project has a file which gradle could use to perform initialization, either of a "single project" or a
   * "multi-project" build.
   *
   * @return {@code true} if the project has a Gradle build or settings file in the project's root folder; {@code false} otherwise.
   */
  public boolean hasTopLevelGradleFile() {
    if (myProject.isDefault()) {
      return false;
    }
    VirtualFile baseDir = myProject.getBaseDir();
    if (baseDir != null) {
      return ((findGradleBuildFile(baseDir) != null) || (findGradleSettingsFile(baseDir) != null));
    }
    return false;
  }

  /**
   * @return the modules in a Gradle-based project that contain an {@code AndroidFacet}.
   */
  @NotNull
  public List<Module> getAndroidModules() {
    ImmutableList.Builder<Module> modules = ImmutableList.builder();

    ReadAction.run(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      for (Module module : ModuleManager.getInstance(myProject).getModules()) {
        if (AndroidFacet.getInstance(module) != null && GradleFacet.getInstance(module) != null) {
          modules.add(module);
        }
      }
    });
    return modules.build();
  }

  /**
   * Applies the given {@link Consumer} to each module in the project that contains an {@code AndroidFacet}.
   *
   * @param consumer the {@code Consumer} to apply.
   */
  public void forEachAndroidModule(@NotNull Consumer<AndroidFacet> consumer) {
    ReadAction.run(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      for (Module module : ModuleManager.getInstance(myProject).getModules()) {
        AndroidFacet androidFacet = AndroidFacet.getInstance(module);
        if (androidFacet != null && GradleFacet.getInstance(module) != null) {
          consumer.consume(androidFacet);
        }
      }
    });
  }

  /**
   * Attempts to retrieve the {@link AndroidModuleModel} for the module containing the given file.
   * <p/>
   * This method will return {@code null} if the file is "excluded" or if the module the file belongs to is not an Android module.
   *
   * @param file the given file.
   * @return the {@code AndroidModuleModel} for the module containing the given file, or {@code null} if the file is "excluded" or if the
   * module the file belongs to is not an Android module.
   */
  @Nullable
  public AndroidModuleModel findAndroidModelInModule(@NotNull VirtualFile file) {
    return findAndroidModelInModule(file, true /* ignore "excluded files */);
  }

  /**
   * Attempts to retrieve the {@link AndroidModuleModel} for the module containing the given file.
   *
   * @param file           the given file.
   * @param honorExclusion if {@code true}, this method will return {@code null} if the given file is "excluded".
   * @return the {@code AndroidModuleModel} for the module containing the given file, or {@code null} if the module is not an Android
   * module.
   */
  @Nullable
  public AndroidModuleModel findAndroidModelInModule(@NotNull VirtualFile file, boolean honorExclusion) {
    Module module = findModuleForFile(file, honorExclusion);
    if (module == null) {
      return null;
    }

    if (module.isDisposed()) {
      getLog().warn("Attempted to get an Android Facet from a disposed module");
      return null;
    }

    return AndroidModuleModel.get(module);
  }

  @Nullable
  private Module findModuleForFile(@NotNull VirtualFile file, boolean honorExclusion) {
    return myProjectFileIndex.getModuleForFile(file, honorExclusion);
  }

  /**
   * Returns the modules to build based on the current selection in the 'Project' tool window. If the module that corresponds to the project
   * is selected, all the modules in such projects are returned. If there is no selection, an empty array is returned.
   *
   * @param dataContext knows the modules that are selected. If {@code null}, this method gets the {@code DataContext} from the 'Project'
   *                    tool window directly.
   * @return the modules to build based on the current selection in the 'Project' tool window.
   */
  @NotNull
  public Module[] getModulesToBuildFromSelection(@Nullable DataContext dataContext) {
    if (dataContext == null) {
      ProjectView projectView = ProjectView.getInstance(myProject);
      AbstractProjectViewPane pane = projectView.getCurrentProjectViewPane();

      if (pane != null) {
        JComponent treeComponent = pane.getComponentToFocus();
        dataContext = DataManager.getInstance().getDataContext(treeComponent);
      }
      else {
        return Module.EMPTY_ARRAY;
      }
    }
    Module[] modules = MODULE_CONTEXT_ARRAY.getData(dataContext);
    if (modules != null) {
      if (modules.length == 1 && isProjectModule(modules[0])) {
        return ModuleManager.getInstance(myProject).getModules();
      }
      return modules;
    }
    Module module = MODULE.getData(dataContext);
    if (module != null) {
      return isProjectModule(module) ? ModuleManager.getInstance(myProject).getModules() : new Module[]{module};
    }
    return Module.EMPTY_ARRAY;
  }

  private static final Key<Set<File>> PROJECTS_BEING_INITIALIZED = Key.create("PROJECTS_BEING_INITIALIZED");

  /**
   * Registers the given location as the location of a new Gradle project which is being initialized.
   *
   * <p>This makes {@link #isBuildWithGradle()} return true for projects at this location.
   *
   * @return an access token which should be finalized when the project is initialized.
   */
  public static AccessToken beginInitializingGradleProjectAt(@NotNull File projectFolderPath) {
    UserDataHolderEx userData = (UserDataHolderEx)ApplicationManager.getApplication();
    Set<File> projectsBeingInitialized = userData.putUserDataIfAbsent(PROJECTS_BEING_INITIALIZED, newConcurrentSet());
    if (!projectsBeingInitialized.add(projectFolderPath)) {
      throw new IllegalStateException(
        "Cannot initialize two projects at the same location at the same time. Project location: " + projectFolderPath);
    }
    return new AccessToken() {
      @Override
      public void finish() {
        projectsBeingInitialized.remove(projectFolderPath);
      }
    };
  }

  private static boolean isBeingInitializedAsGradleProject(@NotNull Project project) {
    String basePath = project.getBasePath();
    if (basePath == null) return false;
    Set<File> projectsBeingInitialized = ApplicationManager.getApplication().getUserData(PROJECTS_BEING_INITIALIZED);
    if (projectsBeingInitialized == null) return false;
    return projectsBeingInitialized.contains(new File(basePath));
  }

  private static boolean isProjectModule(@NotNull Module module) {
    // if we got here is because we are dealing with a Gradle project, but if there is only one module selected and this module is the
    // module that corresponds to the project itself, it won't have an android-gradle facet. In this case we treat it as if we were going
    // to build the whole project.
    File moduleRootFolderPath = findModuleRootFolderPath(module);
    if (moduleRootFolderPath == null) {
      return false;
    }
    String basePath = module.getProject().getBasePath();
    return basePath != null && filesEqual(moduleRootFolderPath, new File(basePath)) && !GradleFacet.isAppliedTo(module);
  }

  @NotNull
  private Logger getLog() {
    return Logger.getInstance(getClass());
  }
}