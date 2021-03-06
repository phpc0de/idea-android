// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.android.tools.idea.gradle.dsl.model.android;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.idea.gradle.dsl.TestFileName;
import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;
import com.android.tools.idea.gradle.dsl.api.android.AndroidModel;
import com.android.tools.idea.gradle.dsl.api.android.BuildTypeModel;
import com.android.tools.idea.gradle.dsl.model.GradleFileModelTestCase;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemDependent;
import org.junit.Test;

/**
 * Tests for {@link BuildTypesDslElement}.
 *
 * <p>
 * In this test, we only test the general structure of {@code android.buildTypes {}}. The build type structure defined by
 * {@link BuildTypeModelImpl} is tested in great deal to cover all combinations in {@link BuildTypeModelTest}.
 */
public class BuildTypesElementTest extends GradleFileModelTestCase {
  @Test
  public void testBuildTypesWithApplicationStatements() throws Exception {
    writeToBuildFile(TestFile.BUILD_TYPES_WITH_APPLICATION_STATEMENTS);

    AndroidModel android = getGradleBuildModel().android();
    assertNotNull(android);

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(4);

    BuildTypeModel buildType1 = buildTypes.get(2);
    assertEquals("applicationIdSuffix", "suffix1", buildType1.applicationIdSuffix());
    assertEquals("proguardFiles", ImmutableList.of("proguard-android-1.txt", "proguard-rules-1.txt"), buildType1.proguardFiles());
    BuildTypeModel buildType2 = buildTypes.get(3);
    assertEquals("applicationIdSuffix", "suffix2", buildType2.applicationIdSuffix());
    assertEquals("proguardFiles", ImmutableList.of("proguard-android-2.txt", "proguard-rules-2.txt"), buildType2.proguardFiles());
  }

  @Test
  public void testBuildTypesWithAssignmentStatements() throws Exception {
    writeToBuildFile(TestFile.BUILD_TYPES_WITH_ASSIGNMENT_STATEMENTS);

    AndroidModel android = getGradleBuildModel().android();
    assertNotNull(android);

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertSize(4, buildTypes);

    BuildTypeModel buildType1 = buildTypes.get(2);
    assertEquals("applicationIdSuffix", "suffix1", buildType1.applicationIdSuffix());
    assertEquals("proguardFiles", ImmutableList.of("proguard-android-1.txt", "proguard-rules-1.txt"), buildType1.proguardFiles());

    BuildTypeModel buildType2 = buildTypes.get(3);
    assertEquals("applicationIdSuffix", "suffix2", buildType2.applicationIdSuffix());
    assertEquals("proguardFiles", ImmutableList.of("proguard-android-2.txt", "proguard-rules-2.txt"), buildType2.proguardFiles());
  }

  @Test
  public void testBuildTypesWithOverrideStatements() throws Exception {
    writeToBuildFile(TestFile.BUILD_TYPES_WITH_OVERRIDE_STATEMENTS);

    AndroidModel android = getGradleBuildModel().android();
    assertNotNull(android);

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(4);

    BuildTypeModel type1 = buildTypes.get(2);
    assertEquals("applicationIdSuffix", "suffix1-1", type1.applicationIdSuffix());
    // TODO(xof): this (and the test below) come from overriding the proguardFiles for a build type, which is straightforward to parse
    //  in Groovy (simple assignment) but not straightforward in Kotlin (requires parsing and data flow analysis of .clear() or
    //  .setProguardFiles()).
    if(isGroovy()) {
      assertEquals("proguardFiles", ImmutableList.of("proguard-android-3.txt", "proguard-rules-3.txt"), type1.proguardFiles());
    }

    BuildTypeModel type2 = buildTypes.get(3);
    assertEquals("applicationIdSuffix", "suffix2-1", type2.applicationIdSuffix());
    if(isGroovy()) {
      assertEquals("proguardFiles", ImmutableList.of("proguard-android-4.txt", "proguard-rules-4.txt"), type2.proguardFiles());
    }
  }

  @Test
  public void testBuildTypesWithAppendStatements() throws Exception {
    writeToBuildFile(TestFile.BUILD_TYPES_WITH_APPEND_STATEMENTS);

    AndroidModel android = getGradleBuildModel().android();
    assertNotNull(android);

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(4);

    BuildTypeModel type1 = buildTypes.get(2);
    assertEquals("proguardFiles",
                 ImmutableList.of("proguard-android-1.txt", "proguard-rules-1.txt", "proguard-android-3.txt", "proguard-rules-3.txt"),
                 type1.proguardFiles());

    BuildTypeModel type2 = buildTypes.get(3);
    assertEquals("proguardFiles", ImmutableList.of("proguard-android-2.txt", "proguard-rules-2.txt", "proguard-android-4.txt"),
                 type2.proguardFiles());
  }

  @Test
  public void testAddEmptyBuildType() throws Exception {
    writeToBuildFile(TestFile.ADD_EMPTY_BUILD_TYPE);

    GradleBuildModel buildModel = getGradleBuildModel();
    AndroidModel android = buildModel.android();
    android.addBuildType("typeA");

    assertTrue(buildModel.isModified());

    applyChangesAndReparse(buildModel);
    verifyFileContents(myBuildFile, TestFile.ADD_EMPTY_BUILD_TYPE_EXPECTED);

    android = buildModel.android();

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(3);

    BuildTypeModel buildType = buildTypes.get(2);
    assertEquals("name", "typeA", buildType.name());
    assertEquals("name", "typeA", buildType.name());
    assertMissingProperty("applicationIdSuffix", buildType.applicationIdSuffix());
    assertEmpty("buildConfigFields", buildType.buildConfigFields());
    assertMissingProperty("consumerProguardFiles", buildType.consumerProguardFiles());
    assertMissingProperty("debuggable", buildType.debuggable());
    assertMissingProperty("embedMicroApp", buildType.embedMicroApp());
    assertMissingProperty("jniDebuggable", buildType.jniDebuggable());
    verifyEmptyMapProperty("manifestPlaceholders", buildType.manifestPlaceholders());
    assertMissingProperty("minifyEnabled", buildType.minifyEnabled());
    assertMissingProperty("multiDexEnabled", buildType.multiDexEnabled());
    assertMissingProperty("proguardFiles", buildType.proguardFiles());
    assertMissingProperty("pseudoLocalesEnabled", buildType.pseudoLocalesEnabled());
    assertMissingProperty("renderscriptDebuggable", buildType.renderscriptDebuggable());
    assertMissingProperty("renderscriptOptimLevel", buildType.renderscriptOptimLevel());
    assertEmpty("resValues", buildType.resValues());
    assertMissingProperty("shrinkResources", buildType.shrinkResources());
    assertMissingProperty("testCoverageEnabled", buildType.testCoverageEnabled());
    assertMissingProperty("useJack", buildType.useJack());
    assertMissingProperty("versionNameSuffix", buildType.versionNameSuffix());
    assertMissingProperty("zipAlignEnabled", buildType.zipAlignEnabled());
  }

  @Test
  public void testAddBuildType() throws Exception {
    writeToBuildFile(TestFile.ADD_BUILD_TYPE);

    GradleBuildModel buildModel = getGradleBuildModel();
    AndroidModel android = buildModel.android();
    android.addBuildType("typeA");
    android.buildTypes().get(2).applicationIdSuffix().setValue("suffixA");

    assertTrue(buildModel.isModified());
    applyChangesAndReparse(buildModel);
    verifyFileContents(myBuildFile, TestFile.ADD_BUILD_TYPE_EXPECTED);

    android = buildModel.android();

    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(3);

    BuildTypeModel buildType = buildTypes.get(2);
    assertEquals("name", "typeA", buildType.name());
    assertEquals("applicationIdSuffix", "suffixA", buildType.applicationIdSuffix());
  }

  @Test
  public void testAddPropertyToImplicitBuildTypes() throws Exception {
    writeToBuildFile(TestFile.ADD_PROPERTY_TO_IMPLICIT_BUILD_TYPES);

    GradleBuildModel buildModel = getGradleBuildModel();
    AndroidModel android = buildModel.android();
    BuildTypeModel debug = android.addBuildType("debug");
    debug.applicationIdSuffix().setValue("-debug");
    BuildTypeModel release = android.addBuildType("release");
    release.applicationIdSuffix().setValue("-release");
    assertTrue(buildModel.isModified());

    applyChangesAndReparse(buildModel);
    verifyFileContents(myBuildFile, TestFile.ADD_PROPERTY_TO_IMPLICIT_BUILD_TYPES_EXPECTED);

    android = buildModel.android();
    List<BuildTypeModel> buildTypes = android.buildTypes();
    assertThat(buildTypes).hasSize(2);
    assertEquals("release.name", "release", buildTypes.get(0).name());
    assertEquals("release.applicationIdSuffix","-release", buildTypes.get(0).applicationIdSuffix());
    assertEquals("debug.name", "debug", buildTypes.get(1).name());
    assertEquals("debug.applicationIdSuffix", "-debug", buildTypes.get(1).applicationIdSuffix());
  }

  @Test
  public void testKnownMethods() throws IOException {
    writeToBuildFile(TestFile.KNOWN_METHODS);

    GradleBuildModel buildModel = getGradleBuildModel();
    List<BuildTypeModel> buildTypes = buildModel.android().buildTypes();
    assertThat(buildTypes).hasSize(2);
    assertEquals("release", buildTypes.get(0).name());
    assertEquals("debug", buildTypes.get(1).name());
  }

  enum TestFile implements TestFileName {
    BUILD_TYPES_WITH_APPLICATION_STATEMENTS("buildTypesWithApplicationStatements"),
    BUILD_TYPES_WITH_ASSIGNMENT_STATEMENTS("buildTypesWithAssignmentStatements"),
    BUILD_TYPES_WITH_OVERRIDE_STATEMENTS("buildTypesWithOverrideStatements"),
    BUILD_TYPES_WITH_APPEND_STATEMENTS("buildTypesWithAppendStatements"),
    ADD_EMPTY_BUILD_TYPE("addEmptyBuildType"),
    ADD_EMPTY_BUILD_TYPE_EXPECTED("addEmptyBuildTypeExpected"),
    ADD_BUILD_TYPE("addBuildType"),
    ADD_BUILD_TYPE_EXPECTED("addBuildTypeExpected"),
    ADD_PROPERTY_TO_IMPLICIT_BUILD_TYPES("addPropertyToImplicitBuildTypes"),
    ADD_PROPERTY_TO_IMPLICIT_BUILD_TYPES_EXPECTED("addPropertyToImplicitBuildTypesExpected"),
    KNOWN_METHODS("knownMethods"),
    ;

    @NotNull private @SystemDependent String path;
    TestFile(@NotNull @SystemDependent String path) {
      this.path = path;
    }

    @NotNull
    @Override
    public File toFile(@NotNull @SystemDependent String basePath, @NotNull String extension) {
      return TestFileName.super.toFile(basePath + "/buildTypesElement/" + path, extension);
    }
  }
}
