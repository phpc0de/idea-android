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

import static com.android.tools.idea.Projects.getBaseDirPath;
import static com.android.tools.idea.testing.Facets.createAndAddGradleFacet;
import static com.android.tools.idea.testing.ProjectFiles.createFileInProjectRoot;
import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.android.tools.idea.testing.IdeComponents;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.PlatformTestCase;
import java.io.File;
import org.easymock.EasyMock;
import org.jetbrains.android.facet.AndroidFacet;

/**
 * Tests for {@link GradleProjectInfo}.
 */
public class GradleProjectInfoTest extends PlatformTestCase {
  private GradleProjectInfo myProjectInfo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProjectInfo = GradleProjectInfo.getInstance(getProject());
  }

  public void testHasTopLevelGradleFileBuildGradle() throws Exception {
    createFileInProjectRoot(getProject(), "build.gradle");
    assertTrue(myProjectInfo.hasTopLevelGradleFile());
  }

  public void testHasTopLevelGradleFileWithBuildGradleKts() throws Exception {
    createFileInProjectRoot(getProject(), "build.gradle.kts");
    assertTrue(myProjectInfo.hasTopLevelGradleFile());
  }

  public void testHasTopLevelGradleFileSettingsGradle() throws Exception {
    createFileInProjectRoot(getProject(), "settings.gradle");
    assertTrue(myProjectInfo.hasTopLevelGradleFile());
  }

  public void testHasTopLevelGradleFileWithSettingsGradleKts() throws Exception {
    createFileInProjectRoot(getProject(), "settings.gradle.kts");
    assertTrue(myProjectInfo.hasTopLevelGradleFile());
  }
  public void testHasTopLevelGradleBuildFileUsingNonGradleProject() {
    File projectFolderPath = getBaseDirPath(getProject());
    String[] filenames = { "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts" };
    for (String filename : filenames ) {
      File buildFilePath = new File(projectFolderPath, filename);
      if (buildFilePath.exists()) {
        assertTrue("Failed to delete top-level " + filename + " file", buildFilePath.delete());
      }
    }
    assertFalse(myProjectInfo.hasTopLevelGradleFile());
  }

  public void testIsBuildWithGradleUsingGradleProject() {
    // Simulate this is a module built with Gradle
    createAndAddGradleFacet(getModule());
    assertTrue(myProjectInfo.isBuildWithGradle());
  }

  public void testIsBuildWithGradleUsingNonGradleProject() {
    // Ensure this module is *not* build by Gradle.
    removeGradleFacetFromModule();

    assertFalse(myProjectInfo.isBuildWithGradle());
  }

  // See https://code.google.com/p/android/issues/detail?id=203384
  public void testIsBuildWithGradleUsingGradleProjectWithoutGradleModules() {
    // Ensure this module is *not* build by Gradle.
    removeGradleFacetFromModule();

    registerLastSyncTimestamp(1L);

    assertTrue(myProjectInfo.isBuildWithGradle());
  }

  // See https://code.google.com/p/android/issues/detail?id=203384
  public void testIsBuildWithGradleUsingProjectWithoutSyncTimestamp() {
    // Ensure this module is *not* build by Gradle.
    removeGradleFacetFromModule();

    registerLastSyncTimestamp(-1L);

    assertFalse(myProjectInfo.isBuildWithGradle());
  }

  public void testGetAndroidModulesUsingGradleProject() {
    // Simulate this is a module built with Gradle
    ApplicationManager.getApplication().runWriteAction(() -> {
      FacetManager facetManager = FacetManager.getInstance(getModule());
      facetManager.addFacet(GradleFacet.getFacetType(), GradleFacet.getFacetName(), null);
      facetManager.addFacet(AndroidFacet.getFacetType(), AndroidFacet.NAME, null);
    });

    assertThat(myProjectInfo.getAndroidModules()).hasSize(1);
    assertEquals(myProjectInfo.getAndroidModules().get(0), getModule());
  }

  public void testGetAndroidModulesUsingNonGradleProject() {
    // Ensure this module is *not* build by Gradle.
    removeGradleFacetFromModule();

    assertEmpty(myProjectInfo.getAndroidModules());
  }

  public void testGetAndroidModulesUsingGradleProjectWithoutGradleModules() {
    // Ensure this module is *not* build by Gradle.
    removeGradleFacetFromModule();

    registerLastSyncTimestamp(1L);

    assertEmpty(myProjectInfo.getAndroidModules());
  }

  public void testHasGradleFacets() {
    createAndAddGradleFacet(getModule());
    assertTrue(myProjectInfo.hasGradleFacets());

    removeGradleFacetFromModule();
    assertFalse(myProjectInfo.hasGradleFacets());
  }

  private void removeGradleFacetFromModule() {
    FacetManager facetManager = FacetManager.getInstance(getModule());
    GradleFacet facet = facetManager.findFacet(GradleFacet.getFacetTypeId(), GradleFacet.getFacetName());
    if (facet != null) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        ModifiableFacetModel facetModel = facetManager.createModifiableModel();
        facetModel.removeFacet(facet);
        facetModel.commit();
      });
    }
  }

  private void registerLastSyncTimestamp(long timestamp) {
    GradleSyncState syncState = mock(GradleSyncState.class);
    new IdeComponents(getProject()).replaceProjectService(GradleSyncState.class, syncState);
    when(syncState.getLastSyncFinishedTimeStamp()).thenReturn(timestamp);
  }

  public void testGetSelectedModules() {
    createAndAddGradleFacet(myModule);

    DataContext dataContext = createMock(DataContext.class);
    Module[] data = {myModule};
    expect(dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY.getName())).andReturn(data);

    replay(dataContext);

    Module[] selectedModules = GradleProjectInfo.getInstance(myProject).getModulesToBuildFromSelection(dataContext);
    assertSame(data, selectedModules);

    EasyMock.verify(dataContext);
  }

  public void testGetSelectedModulesWithModuleWithoutAndroidGradleFacet() {
    DataContext dataContext = createMock(DataContext.class);
    Module[] data = {myModule};
    expect(dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY.getName())).andReturn(data);

    replay(dataContext);

    Module[] selectedModules = GradleProjectInfo.getInstance(myProject).getModulesToBuildFromSelection(dataContext);
    assertNotSame(data, selectedModules);
    assertEquals(1, selectedModules.length);
    assertSame(myModule, selectedModules[0]);

    EasyMock.verify(dataContext);
  }
}