/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.gradle.dependencies;

import static com.android.SdkConstants.SUPPORT_LIB_GROUP_ID;
import static com.android.tools.idea.gradle.dsl.api.dependencies.CommonConfigurationNames.COMPILE;
import static com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger.TRIGGER_GRADLEDEPENDENCY_ADDED;
import static com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger.TRIGGER_GRADLEDEPENDENCY_UPDATED;
import static com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel;
import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.openapi.util.text.StringUtil.pluralize;

import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.GradleVersion;
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.ArtifactDependencyModel;
import com.android.tools.idea.gradle.dsl.api.dependencies.DependenciesModel;
import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker;
import com.android.tools.idea.gradle.project.build.invoker.GradleInvocationResult;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.gradle.repositories.RepositoryUrlManager;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.google.common.base.Objects;
import com.google.wireless.android.sdk.stats.GradleSyncStats;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleDependencyManager {
  private static final String ADD_DEPENDENCY = "Add Dependency";

  @NotNull
  public static GradleDependencyManager getInstance(@NotNull Project project) {
    return project.getService(GradleDependencyManager.class);
  }

  /**
   * Returns the dependencies that are NOT defined in the build files.
   *
   * Note: A dependency is still regarded as missing even if it's available
   * by a transitive dependency.
   * Also: the version of the dependency is disregarded.
   *
   * @param module       the module to check dependencies in
   * @param dependencies the dependencies of interest.
   * @return a list of the dependencies NOT defined in the build files.
   */
  @NotNull
  public List<GradleCoordinate> findMissingDependencies(@NotNull Module module, @NotNull Iterable<GradleCoordinate> dependencies) {
    AndroidModuleModel gradleModel = AndroidModuleModel.get(module);
    GradleBuildModel buildModel = GradleBuildModel.get(module);

    if (gradleModel == null && buildModel == null) {
      return Collections.emptyList();
    }

    List<ArtifactDependencyModel> compileDependencies = buildModel != null ? buildModel.dependencies().artifacts() : null;

    // Record current version of support library; if used, prefer that for other dependencies
    // (e.g. if you're using appcompat-v7 version 25.3.1, and you drag in a recyclerview-v7
    // library, we should also use 25.3.1, not whatever happens to be latest
    GradleVersion appCompatVersion = null;
    if (compileDependencies != null) {
      for (ArtifactDependencyModel dependency : compileDependencies) {
        if (Objects.equal(SUPPORT_LIB_GROUP_ID, dependency.group().toString()) &&
            !Objects.equal("multidex", dependency.name().forceString())) {
          String s = dependency.version().toString();
          if (s != null) {
            appCompatVersion = GradleVersion.tryParse(s);
          }
          break;
        }
      }
    }

    Project project = module.getProject();
    RepositoryUrlManager manager = RepositoryUrlManager.get();
    List<GradleCoordinate> missingLibraries = new ArrayList<>();
    for (GradleCoordinate coordinate : dependencies) {
      String groupId = coordinate.getGroupId();
      String artifactId = coordinate.getArtifactId();
      if (artifactId == null || groupId == null) {
        // We don't have enough info to continue. Skip.
        continue;
      }

      GradleCoordinate resolvedCoordinate = manager.resolveDynamicCoordinate(coordinate, project, null);

      // If we're adding a support library with a dynamic version (+), and we already have a resolved
      // support library version, use that specific version for the new support library too to keep them
      // all consistent.
      if (appCompatVersion != null
          && coordinate.acceptsGreaterRevisions() && SUPPORT_LIB_GROUP_ID.equals(groupId)
          // The only library in groupId=SUPPORT_LIB_GROUP_ID which doesn't follow the normal version numbering scheme
          && !artifactId.equals("multidex")) {
        resolvedCoordinate = GradleCoordinate.parseCoordinateString(groupId + ":" + artifactId + ":" + appCompatVersion.toString());
      }

      if (resolvedCoordinate != null) {
        coordinate = resolvedCoordinate;
      }

      boolean dependencyFound = compileDependencies != null &&
                                compileDependencies.stream()
                                                   .anyMatch(d -> Objects.equal(d.group().toString(), groupId) &&
                                                                  d.name().forceString().equals(artifactId));
      if (!dependencyFound) {
        missingLibraries.add(coordinate);
      }
    }

    return missingLibraries;
  }

  /**
   * Add all the specified dependencies to the module. Adding a dependency that already exists will result in a no-op.
   * A sync will be triggered immediately after a successful addition (e.g. [dependencies] contains a dependency that
   * doesn't already exist and is therefore added); and caller may supply a callback to determine when the requested
   * dependencies have been added (this make take several seconds).
   *
   * @param module       the module to add dependencies to
   * @param dependencies the dependencies of interest
   * @param callback     an optional callback to signal to completion of the added dependencies
   * @return true if the dependencies were successfully added or were already present in the module
   */
  public boolean addDependenciesAndSync(@NotNull Module module,
                                        @NotNull Iterable<GradleCoordinate> dependencies,
                                        @Nullable Runnable callback) {
    return addDependenciesInTransaction(module, dependencies, true, callback, null);
  }

  /**
   * Add all the specified dependencies to the module without triggering a sync afterwards.
   * Adding a dependency that already exists will result in a no-op.
   *
   * @param module       the module to add dependencies to
   * @param dependencies the dependencies of interest
   * @return true if the dependencies were successfully added or were already present in the module.
   */
  public boolean addDependenciesWithoutSync(@NotNull Module module, @NotNull Iterable<GradleCoordinate> dependencies) {
    return addDependenciesInTransaction(module, dependencies, false, null, null);
  }

  /**
   * Like {@link #addDependenciesWithoutSync(Module, Iterable)} but allows you to customize the configuration
   * name of the inserted dependencies.
   *
   * @param module       the module to add dependencies to
   * @param dependencies the dependencies of interest
   * @param nameMapper   a factory to produce configuration names and artifact specsc
   * @return true if the dependencies were successfully added or were already present in the module.
   */
  public boolean addDependenciesWithoutSync(
    @NotNull Module module,
    @NotNull Iterable<GradleCoordinate> dependencies,
    @Nullable ConfigurationNameMapper nameMapper) {
    return addDependenciesInTransaction(module, dependencies, false, null, nameMapper);
  }

  /**
   * Updates any coordinates to the versions specified in the dependencies list.
   * For example, if you pass it [com.android.support.constraint:constraint-layout:1.0.0-alpha2],
   * it will find any constraint layout occurrences of 1.0.0-alpha1 and replace them with 1.0.0-alpha2.
   */
  public boolean updateLibrariesToVersion(@NotNull Module module,
                                          @NotNull List<GradleCoordinate> dependencies,
                                          @Nullable Runnable callback) {
    GradleBuildModel buildModel = GradleBuildModel.get(module);
    if (buildModel == null) {
      return false;
    }
    updateDependenciesInTransaction(buildModel, module, dependencies, callback);
    return true;
  }

  public boolean userWantToAddDependencies(@NotNull Module module, @NotNull Collection<GradleCoordinate> missing) {
    String libraryNames = join(missing, GradleCoordinate::getArtifactId, ", ");
    String message = String.format("This operation requires the %1$s %2$s. \n\nWould you like to add %3$s %1$s now?",
                                   pluralize("library", missing.size()), libraryNames, pluralize("this", missing.size()));
    Project project = module.getProject();
    return Messages.showOkCancelDialog(project, message, "Add Project Dependency", Messages.getErrorIcon()) == Messages.OK;
  }

  private boolean addDependenciesInTransaction(@NotNull Module module,
                                               @NotNull Iterable<GradleCoordinate> coordinates,
                                               boolean performSync,
                                               @Nullable Runnable callback,
                                               @Nullable ConfigurationNameMapper nameMapper) {
    // callback method should never be provided when a sync is not requested.
    if (!performSync && callback != null) {
      throw new IllegalArgumentException("Callback must be null if sync is not requested.");
    }

    GradleBuildModel buildModel = GradleBuildModel.get(module);
    if (buildModel == null) {
      return false;
    }

    Project project = module.getProject();
    WriteCommandAction.writeCommandAction(project).withName(ADD_DEPENDENCY).run(() -> {
      List<GradleCoordinate> missing = findMissingDependencies(module, coordinates);
      if (missing.isEmpty()) {
        return;
      }

      addDependencies(buildModel, module, missing, nameMapper);

      if (performSync) {
        requestProjectSync(project, callback, TRIGGER_GRADLEDEPENDENCY_ADDED);
      }
    });
    return true;
  }

  private static void addDependencies(@NotNull GradleBuildModel buildModel,
                                      @NotNull Module module,
                                      @NotNull List<GradleCoordinate> coordinates,
                                      @Nullable ConfigurationNameMapper nameMapper) {
    updateModel(module, model -> {
      DependenciesModel dependenciesModel = buildModel.dependencies();
      for (GradleCoordinate coordinate : coordinates) {
        String name = COMPILE;
        if (nameMapper != null) {
          name = nameMapper.mapName(module, name, coordinate);
        }
        name = GradleUtil.mapConfigurationName(name, GradleUtil.getAndroidGradleModelVersionInUse(module), false);
        dependenciesModel.addArtifact(name, coordinate.toString());
      }
      buildModel.applyChanges();
    });
  }

  private static void updateDependenciesInTransaction(@NotNull GradleBuildModel buildModel,
                                                      @NotNull Module module,
                                                      @NotNull List<GradleCoordinate> coordinates,
                                                      @Nullable Runnable callback) {
    assert !coordinates.isEmpty();

    Project project = module.getProject();
    WriteCommandAction.writeCommandAction(project).withName(ADD_DEPENDENCY).run(() -> {
      updateDependencies(buildModel, module, coordinates);
      requestProjectSync(project, callback, TRIGGER_GRADLEDEPENDENCY_UPDATED);
    });
  }

  private static void requestProjectSync(@NotNull Project project, @Nullable Runnable callback, @NotNull GradleSyncStats.Trigger trigger) {
    if (callback != null) {
      // Note: This callback mechanism fires after the Gradle build is done rather than the sync.
      // This is needed since the designer cannot display correctly with source generation.
      GradleBuildInvoker.getInstance(project).add(new GradleCompletionTask(project, callback));
    }
    GradleSyncInvoker.Request request = new GradleSyncInvoker.Request(trigger);
    GradleSyncInvoker.getInstance().requestProjectSync(project, request);
  }

  private static void updateDependencies(@NotNull GradleBuildModel buildModel,
                                         @NotNull Module module,
                                         @NotNull List<GradleCoordinate> coordinates) {
    updateModel(module, model -> {
      DependenciesModel dependenciesModel = buildModel.dependencies();
      for (GradleCoordinate gc : coordinates) {
        List<ArtifactDependencyModel> artifacts = new ArrayList<>(dependenciesModel.artifacts());
        for (ArtifactDependencyModel m : artifacts) {
          if (gc.getGroupId() != null && gc.getGroupId().equals(m.group().toString())
              && gc.getArtifactId() != null && gc.getArtifactId().equals(m.name().forceString())
              && !gc.getRevision().equals(m.version().toString())) {
            dependenciesModel.remove(m);
            dependenciesModel.addArtifact(m.configurationName(), gc.toString());
          }
        }
      }
      buildModel.applyChanges();
    });
  }

  private static class GradleCompletionTask implements GradleBuildInvoker.AfterGradleInvocationTask {
    private final Project myProject;
    private final Runnable myCallback;

    private GradleCompletionTask(@NotNull Project project, @NotNull Runnable callback) {
      myProject = project;
      myCallback = callback;
    }

    @Override
    public void execute(@NotNull GradleInvocationResult result) {
      GradleBuildInvoker.getInstance(myProject).remove(this);
      myCallback.run();
    }
  }
}
