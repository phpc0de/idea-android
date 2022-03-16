/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.run;

import static com.android.tools.idea.testartifacts.TestConfigurationTesting.createAndroidTestConfigurationFromClass;
import static com.android.tools.idea.testing.TestProjectPaths.DYNAMIC_APP;
import static com.android.tools.idea.testing.TestProjectPaths.PROJECT_WITH_APP_AND_LIB_DEPENDENCY;
import static com.google.common.truth.Truth.assertThat;

import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.android.tools.idea.testing.AndroidGradleTestUtilsKt;
import java.util.List;
import java.util.stream.Collectors;

public class AndroidTestRunConfigurationTest extends AndroidGradleTestCase {

  private static final String TEST_APP_CLASS_NAME = "google.simpleapplication.ApplicationTest";
  private static final String DYNAMIC_FEATURE_INSTRUMENTED_TEST_CLASS_NAME = "com.example.instantapp.ExampleInstrumentedTest";

  public void testApkProviderForPreLDevice() throws Exception {
    loadProject(DYNAMIC_APP);

    AndroidRunConfigurationBase androidTestRunConfiguration =
      createAndroidTestConfigurationFromClass(getProject(), TEST_APP_CLASS_NAME);
    assertNotNull(androidTestRunConfiguration);

    ApkProvider provider = androidTestRunConfiguration.getApkProvider();
    assertThat(provider).isNotNull();
    assertThat(provider).isInstanceOf(GradleApkProvider.class);
    assertThat(((GradleApkProvider)provider).isTest()).isTrue();
    assertThat(((GradleApkProvider)provider).getOutputKind(new AndroidVersion(19)))
      .isEqualTo(GradleApkProvider.OutputKind.AppBundleOutputModel);
  }

  public void testApkProviderForPostLDevice() throws Exception {
    loadProject(DYNAMIC_APP);

    AndroidRunConfigurationBase androidTestRunConfiguration =
      createAndroidTestConfigurationFromClass(getProject(), TEST_APP_CLASS_NAME);
    assertNotNull(androidTestRunConfiguration);

    ApkProvider provider = androidTestRunConfiguration.getApkProvider();
    assertThat(provider).isNotNull();
    assertThat(provider).isInstanceOf(GradleApkProvider.class);
    assertThat(((GradleApkProvider)provider).isTest()).isTrue();
    assertThat(((GradleApkProvider)provider).getOutputKind(new AndroidVersion(24))).isEqualTo(GradleApkProvider.OutputKind.Default);
  }

  public void testApkProviderForDynamicFeatureInstrumentedTest() throws Exception {
    loadProject(DYNAMIC_APP);

    AndroidRunConfigurationBase androidTestRunConfiguration =
      createAndroidTestConfigurationFromClass(getProject(), DYNAMIC_FEATURE_INSTRUMENTED_TEST_CLASS_NAME);
    assertNotNull(androidTestRunConfiguration);

    ApkProvider provider = androidTestRunConfiguration.getApkProvider();
    assertThat(provider).isNotNull();
    assertThat(provider).isInstanceOf(GradleApkProvider.class);
    assertThat(((GradleApkProvider)provider).isTest()).isTrue();
    assertThat(((GradleApkProvider)provider).getOutputKind(new AndroidVersion(24)))
      .isEqualTo(GradleApkProvider.OutputKind.AppBundleOutputModel);
  }

  public void testCannotRunLibTestsInReleaseBuild() throws Exception {
    loadProject(PROJECT_WITH_APP_AND_LIB_DEPENDENCY);

    AndroidRunConfigurationBase androidTestRunConfiguration =
      createAndroidTestConfigurationFromClass(getProject(), "com.example.projectwithappandlib.lib.ExampleInstrumentedTest");
    assertNotNull(androidTestRunConfiguration);

    List<ValidationError> errors = androidTestRunConfiguration.validate(null);
    assertThat(errors).hasSize(0);

    AndroidGradleTestUtilsKt.switchVariant(getProject(), ":app", "basicRelease");
    errors = androidTestRunConfiguration.validate(null);
    assertThat(errors).isNotEmpty();
    assertThat(errors.stream().map(ValidationError::getMessage).collect(Collectors.toList()))
      .contains("Active build variant \"release\" does not have a test artifact.");
 }
}
