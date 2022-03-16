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
package com.android.tools.idea.gradle.project.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.serialization.PropertyMapping;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.kotlin.idea.gradleTooling.model.kapt.KaptGradleModel;

import static com.android.SdkConstants.GRADLE_PATH_SEPARATOR;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;

/**
 * Module-level Gradle information.
 */
public class GradleModuleModel implements ModuleModel {
  // Increase the value when adding/removing fields or when changing the serialization/deserialization mechanism.
  private static final long serialVersionUID = 4L;

  @NotNull private final String myModuleName;
  @NotNull private final List<String> myTaskNames;
  @NotNull private final String myGradlePath;
  @NotNull private final File myRootFolderPath;
  @NotNull private final List<String> myGradlePlugins;

  @Nullable private final File myBuildFilePath;
  @Nullable private final String myGradleVersion;
  @Nullable private final String myAgpVersion;

  private final boolean myIsKaptEnabled;

  /**
   * @param moduleName    the name of the IDE module.
   * @param gradleProject the model obtained from Gradle.
   * @param gradlePlugins the list of gradle plugins applied to this module.
   * @param buildFilePath the path of the build.gradle file.
   * @param gradleVersion the version of Gradle used to sync the project.
   * @param agpVersion    the version of AGP used to sync the project.
   */
  public GradleModuleModel(@NotNull String moduleName,
                           @NotNull GradleProject gradleProject,
                           @NotNull Collection<String> gradlePlugins,
                           @Nullable File buildFilePath,
                           @Nullable String gradleVersion,
                           @Nullable String agpVersion,
                           @Nullable KaptGradleModel kaptGradleModel) {
    this(moduleName, getTaskNames(gradleProject), gradleProject.getPath(),
         gradleProject.getProjectIdentifier().getBuildIdentifier().getRootDir(), ImmutableList.copyOf(gradlePlugins), buildFilePath,
         gradleVersion, agpVersion, kaptGradleModel != null && kaptGradleModel.isEnabled());
  }

  /**
   * Method of constructing a GradleModuleModel without a GradleProject for use in tests ONLY.
   */
  @VisibleForTesting
  @PropertyMapping({
    "myModuleName",
    "myTaskNames",
    "myGradlePath",
    "myRootFolderPath",
    "myGradlePlugins",
    "myBuildFilePath",
    "myGradleVersion",
    "myAgpVersion",
    "myIsKaptEnabled"
  })
  public GradleModuleModel(@NotNull String moduleName,
                           @NotNull List<String> taskNames,
                           @NotNull String gradlePath,
                           @NotNull File rootFolderPath,
                           @NotNull List<String> gradlePlugins,
                           @Nullable File buildFilePath,
                           @Nullable String gradleVersion,
                           @Nullable String agpVersion,
                           boolean isKaptEnabled) {
    myModuleName = moduleName;
    myTaskNames = taskNames;
    myGradlePath = gradlePath;
    myRootFolderPath = rootFolderPath;
    myGradlePlugins = gradlePlugins;
    myBuildFilePath = buildFilePath;
    myGradleVersion = gradleVersion;
    myAgpVersion = agpVersion;
    myIsKaptEnabled = isKaptEnabled;
  }

  @NotNull
  private static List<String> getTaskNames(@NotNull GradleProject gradleProject) {
    List<String> taskNames = new ArrayList<>();
    DomainObjectSet<? extends GradleTask> tasks = gradleProject.getTasks();
    if (!tasks.isEmpty()) {
      for (GradleTask task : tasks) {
        String name = task.getName();
        if (isNotEmpty(name)) {
          taskNames.add(task.getProject().getPath() + GRADLE_PATH_SEPARATOR + task.getName());
        }
      }
    }
    return taskNames;
  }

  @Override
  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  /**
   * @return the path of the Gradle project.
   */
  @NotNull
  public String getGradlePath() {
    return myGradlePath;
  }

  @NotNull
  public File getRootFolderPath() {
    return myRootFolderPath;
  }

  @NotNull
  public List<String> getTaskNames() {
    return myTaskNames;
  }

  @Nullable
  public VirtualFile getBuildFile() {
    return myBuildFilePath != null ? findFileByIoFile(myBuildFilePath, true) : null;
  }

  @Nullable
  public File getBuildFilePath() {
    return myBuildFilePath;
  }

  @Nullable
  public String getGradleVersion() {
    return myGradleVersion;
  }

  @Nullable
  public String getAgpVersion() {
    return myAgpVersion;
  }

  @NotNull
  public List<String> getGradlePlugins() {
    return myGradlePlugins;
  }

  public boolean isKaptEnabled() {
    return myIsKaptEnabled;
  }
}
