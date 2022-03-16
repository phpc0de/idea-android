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
package com.android.tools.idea.gradle.project.sync.setup.module.idea.java;

import static com.android.tools.idea.testing.Facets.createAndAddGradleFacet;
import static com.android.tools.idea.testing.Facets.createAndAddJavaFacet;
import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.tools.idea.gradle.project.facet.java.JavaFacet;
import com.android.tools.idea.gradle.project.facet.java.JavaFacetConfiguration;
import com.android.tools.idea.gradle.project.model.JavaModuleModel;
import com.android.tools.idea.gradle.project.sync.ModuleSetupContext;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.PlatformTestCase;
import java.io.File;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tests for {@link JavaFacetModuleSetupStep}.
 */
public class JavaFacetModuleSetupStepTest extends PlatformTestCase {
  private IdeModifiableModelsProvider myModelsProvider;
  private JavaFacetModuleSetupStep mySetupStep;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myModelsProvider = ProjectDataManager.getInstance().createModifiableModelsProvider(getProject());
    mySetupStep = new JavaFacetModuleSetupStep();
  }

  public void testDoSetUpModuleWithExistingJavaFacet() throws IOException {
    createAndAddGradleFacet(getModule());

    JavaFacet facet = createAndAddJavaFacet(getModule());
    File buildFolderPath = createTempDir("build");
    boolean buildable = true;

    JavaModuleModel javaModel = mock(JavaModuleModel.class);
    when(javaModel.getBuildFolderPath()).thenReturn(buildFolderPath);
    when(javaModel.isBuildable()).thenReturn(buildable);

    Module module = getModule();
    ModuleSetupContext context = new ModuleSetupContext.Factory().create(module, myModelsProvider);
    mySetupStep.doSetUpModule(context, javaModel);

    ApplicationManager.getApplication().runWriteAction(() -> myModelsProvider.commit());

    // JavaFacet should be reused.
    assertSame(facet, findJavaFacet(module));

    verifyFacetConfiguration(facet, javaModel, buildFolderPath, buildable);
  }

  public void testDoSetUpModuleWithNewJavaFacet() throws IOException {
    createAndAddGradleFacet(getModule());

    File buildFolderPath = createTempDir("build");
    boolean buildable = true;

    JavaModuleModel javaModel = mock(JavaModuleModel.class);
    when(javaModel.getBuildFolderPath()).thenReturn(buildFolderPath);
    when(javaModel.isBuildable()).thenReturn(buildable);

    Module module = getModule();
    ModuleSetupContext context = new ModuleSetupContext.Factory().create(module, myModelsProvider);
    mySetupStep.doSetUpModule(context, javaModel);

    ApplicationManager.getApplication().runWriteAction(() -> myModelsProvider.commit());

    JavaFacet facet = findJavaFacet(module);
    assertNotNull(facet);

    verifyFacetConfiguration(facet, javaModel, buildFolderPath, buildable);
  }

  @Nullable
  private static JavaFacet findJavaFacet(@NotNull Module module) {
    FacetManager facetManager = FacetManager.getInstance(module);
    return facetManager.findFacet(JavaFacet.getFacetTypeId(), JavaFacet.getFacetName());
  }

  private static void verifyFacetConfiguration(@NotNull JavaFacet facet,
                                               @NotNull JavaModuleModel javaModel,
                                               @NotNull File buildFolderPath,
                                               boolean buildable) {
    assertSame(javaModel, facet.getJavaModuleModel());
    JavaFacetConfiguration configuration = facet.getConfiguration();
    assertEquals(buildable, configuration.BUILDABLE);

    verify(javaModel).isBuildable();
  }

  public void testDoSetUpModuleWithoutGradleFacet() throws IOException {
    File buildFolderPath = createTempDir("build");
    boolean buildable = true;

    JavaModuleModel javaModel = mock(JavaModuleModel.class);
    when(javaModel.getBuildFolderPath()).thenReturn(buildFolderPath);
    when(javaModel.isBuildable()).thenReturn(buildable);

    Module module = getModule();
    ModuleSetupContext context = new ModuleSetupContext.Factory().create(module, myModelsProvider);
    mySetupStep.doSetUpModule(context, javaModel);

    ApplicationManager.getApplication().runWriteAction(() -> myModelsProvider.commit());

    JavaFacet facet = findJavaFacet(module);
    assertNotNull(facet);
    assertNull(facet.getJavaModuleModel());
  }
}