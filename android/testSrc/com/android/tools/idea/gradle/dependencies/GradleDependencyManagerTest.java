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
package com.android.tools.idea.gradle.dependencies;

import static com.android.ide.common.rendering.api.ResourceNamespace.RES_AUTO;
import static com.android.tools.idea.projectsystem.gradle.GradleModuleSystemKt.CHECK_DIRECT_GRADLE_DEPENDENCIES;
import static com.android.tools.idea.testing.TestProjectPaths.SIMPLE_APP_WITH_OLDER_SUPPORT_LIB;
import static com.android.tools.idea.testing.TestProjectPaths.SPLIT_BUILD_FILES;
import static com.google.common.truth.Truth.assertThat;

import com.android.SdkConstants;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.resources.ResourceItem;
import com.android.resources.ResourceType;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.gradle.util.GradleUtil;
import com.android.tools.idea.projectsystem.GoogleMavenArtifactId;
import com.android.tools.idea.projectsystem.ProjectSystemUtil;
import com.android.tools.idea.res.ResourceRepositoryManager;
import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.android.tools.idea.testing.TestModuleUtil;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link GradleDependencyManager}.
 */
public class GradleDependencyManagerTest extends AndroidGradleTestCase {
  private static final GradleCoordinate APP_COMPAT_DEPENDENCY = new GradleCoordinate("com.android.support", "appcompat-v7", "+");
  private static final GradleCoordinate RECYCLER_VIEW_DEPENDENCY = new GradleCoordinate("com.android.support", "recyclerview-v7", "+");
  private static final GradleCoordinate DUMMY_DEPENDENCY = new GradleCoordinate("dummy.group", "dummy.artifact", "0.0.0");
  private static final GradleCoordinate VECTOR_DRAWABLE_DEPENDENCY =
    new GradleCoordinate("com.android.support", "support-vector-drawable", "+");

  private static final List<GradleCoordinate> DEPENDENCIES = ImmutableList.of(APP_COMPAT_DEPENDENCY, DUMMY_DEPENDENCY);

  public void testFindMissingDependenciesWithRegularProject() throws Exception {
    loadSimpleApplication();
    Module appModule = TestModuleUtil.findAppModule(getProject());
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> missingDependencies = dependencyManager.findMissingDependencies(appModule, DEPENDENCIES);
    assertThat(missingDependencies).containsExactly(DUMMY_DEPENDENCY);
  }

  public void testFindMissingDependenciesInProjectWithSplitBuildFiles() throws Exception {
    loadProject(SPLIT_BUILD_FILES);
    Module appModule = TestModuleUtil.findAppModule(getProject());
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> missingDependencies = dependencyManager.findMissingDependencies(appModule, DEPENDENCIES);
    assertThat(missingDependencies).containsExactly(DUMMY_DEPENDENCY);
  }

  @SuppressWarnings("unused")
  public void ignore_testDependencyAarIsExplodedForLayoutLib() throws Exception {
    loadSimpleApplication();

    Module appModule = TestModuleUtil.findAppModule(getProject());
    List<GradleCoordinate> dependencies = Collections.singletonList(RECYCLER_VIEW_DEPENDENCY);
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isNotEmpty();

    boolean found = dependencyManager.addDependenciesAndSync(appModule, dependencies, null);
    assertTrue(found);

    // @formatter:off
    List<ResourceItem> items = ResourceRepositoryManager.getAppResources(myAndroidFacet)
                                                    .getResources(RES_AUTO, ResourceType.STYLEABLE, "RecyclerView");
    // @formatter:on
    assertThat(items).isNotEmpty();
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isEmpty();
  }

  @SuppressWarnings("unused")
  public void ignore_testAddDependencyAndSync() throws Exception {
    loadSimpleApplication();
    Module appModule = TestModuleUtil.findAppModule(getProject());
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> dependencies = Collections.singletonList(RECYCLER_VIEW_DEPENDENCY);

    // Setup:
    // 1. RecyclerView artifact should not be declared in build script.
    // 2. RecyclerView should not be declared or resolved.
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isNotEmpty();
    assertFalse(isRecyclerViewRegistered());
    assertFalse(isRecyclerViewResolved());

    boolean result = dependencyManager.addDependenciesAndSync(appModule, dependencies, null);

    // If addDependencyAndSync worked correctly,
    // 1. findMissingDependencies with the added dependency should return empty.
    // 2. RecyclerView should be declared and resolved (because the required artifact has been synced)
    assertTrue(result);
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isEmpty();
    assertTrue(isRecyclerViewRegistered());
    assertTrue(isRecyclerViewResolved());
  }

  @SuppressWarnings("unused")
  public void ignore_testAddDependencyWithoutSync() throws Exception {
    if (!CHECK_DIRECT_GRADLE_DEPENDENCIES) {
      // TODO: b/129297171
      // For now: We are not checking direct dependencies.
      // Re-enable this test when removing this variable.
      return;
    }
    loadSimpleApplication();
    Module appModule = TestModuleUtil.findAppModule(getProject());
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> dependencies = Collections.singletonList(RECYCLER_VIEW_DEPENDENCY);

    // Setup:
    // 1. RecyclerView artifact should not be declared in build script.
    //    // 2. RecyclerView should not be declared or resolved.
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isNotEmpty();
    assertFalse(isRecyclerViewRegistered());
    assertFalse(isRecyclerViewResolved());

    boolean result = dependencyManager.addDependenciesWithoutSync(appModule, dependencies);

    // If addDependencyWithoutSync worked correctly,
    // 1. findMissingDependencies with the added dependency should return empty.
    // 2. RecyclerView should be declared but NOT yet resolved (because we didn't sync)
    assertTrue(result);
    assertThat(dependencyManager.findMissingDependencies(appModule, dependencies)).isEmpty();
    assertTrue(isRecyclerViewRegistered());
    assertFalse(isRecyclerViewResolved());
  }

  public void testAddedSupportDependencySameVersionAsExistingSupportDependency() throws Exception {
    // Load a library with an explicit appcompat-v7 version that is older than the most recent version:
    loadProject(SIMPLE_APP_WITH_OLDER_SUPPORT_LIB);

    Module appModule = TestModuleUtil.findAppModule(getProject());
    List<GradleCoordinate> dependencies = ImmutableList.of(APP_COMPAT_DEPENDENCY, RECYCLER_VIEW_DEPENDENCY);
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> missing = dependencyManager.findMissingDependencies(appModule, dependencies);
    assertThat(missing.size()).isEqualTo(1);
    assertThat(missing.get(0).getId()).isEqualTo(SdkConstants.RECYCLER_VIEW_LIB_ARTIFACT);
    assertThat(missing.get(0).toString()).isEqualTo("com.android.support:recyclerview-v7:25.4.0");
  }

  public void testCanAddDependencyWhichAlreadyIsAnIndirectDependency() throws Exception {
    loadSimpleApplication();

    Module appModule = TestModuleUtil.findAppModule(getProject());
    // Make sure the app module depends on the vector drawable library:
    AndroidModuleModel gradleModel = AndroidModuleModel.get(appModule);
    assertTrue(GradleUtil.dependsOn(gradleModel, VECTOR_DRAWABLE_DEPENDENCY.getId()));

    // Now check that the vector drawable library is NOT an explicit dependency:
    List<GradleCoordinate> vectorDrawable = Collections.singletonList(VECTOR_DRAWABLE_DEPENDENCY);
    GradleDependencyManager dependencyManager = GradleDependencyManager.getInstance(getProject());
    List<GradleCoordinate> missing = dependencyManager.findMissingDependencies(appModule, vectorDrawable);
    assertFalse(missing.isEmpty());
  }

  private boolean isRecyclerViewRegistered() {
    return ProjectSystemUtil.getModuleSystem(TestModuleUtil.findAppModule(getProject()))
             .getRegisteredDependency(GoogleMavenArtifactId.RECYCLERVIEW_V7.getCoordinate("+")) != null;
  }

  private boolean isRecyclerViewResolved() {
    return ProjectSystemUtil.getModuleSystem(TestModuleUtil.findAppModule(getProject()))
             .getResolvedDependency(GoogleMavenArtifactId.RECYCLERVIEW_V7.getCoordinate("+")) != null;
  }
}
