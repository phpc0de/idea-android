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
package com.android.tools.idea.projectsystem;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.tools.idea.gradle.dependencies.GradleDependencyManager;
import com.android.tools.idea.gradle.project.GradleProjectInfo;
import com.android.tools.idea.gradle.project.build.GradleProjectBuilder;
import com.android.tools.idea.projectsystem.gradle.GradleProjectSystem;
import com.android.tools.idea.testing.IdeComponents;
import com.intellij.testFramework.PlatformTestCase;

public class GradleProjectSystemTest extends PlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    IdeComponents ideComponents = new IdeComponents(myProject);
    ideComponents.mockProjectService(GradleDependencyManager.class);
    ideComponents.mockProjectService(GradleProjectBuilder.class);

    GradleProjectInfo gradleProjectInfo = ideComponents.mockProjectService(GradleProjectInfo.class);
    when(gradleProjectInfo.isBuildWithGradle()).thenReturn(true);
  }

  public void testIsGradleProjectSystem() {
    assertThat(ProjectSystemUtil.getProjectSystem(getProject())).isInstanceOf(GradleProjectSystem.class);
  }

  public void testCompileProject() {
    ProjectSystemUtil.getProjectSystem(getProject()).getBuildManager().compileProject();
    verify(GradleProjectBuilder.getInstance(myProject)).compileJava();
  }
}
