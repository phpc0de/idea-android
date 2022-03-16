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
package com.android.tools.idea.gradle.project.sync.snapshots

import com.android.tools.idea.gradle.project.sync.internal.dumpLintModels
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.GradleIntegrationTest
import com.android.tools.idea.testing.SnapshotComparisonTest
import com.android.tools.idea.testing.TestProjectToSnapshotPaths
import com.android.tools.idea.testing.assertIsEqualToSnapshot
import com.android.tools.idea.testing.onEdt
import com.android.tools.idea.testing.openPreparedProject
import com.android.tools.idea.testing.prepareGradleProject
import com.android.tools.idea.testing.saveAndDump
import com.intellij.testFramework.RunsInEdt
import com.intellij.util.PathUtil
import org.jetbrains.android.AndroidTestBase
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * Snapshot tests for 'Lint Models'.
 *
 * These tests convert Lint models to a stable text format which does not depend on local
 * environment (and ideally should not depend on the versions of irrelevant libraries) and compare them to pre-recorded golden
 * results.
 *
 * The pre-recorded sync results can be found in [snapshotDirectoryWorkspaceRelativePath] *.txt files.
 *
 * NOTE: It you made changes to sync or the test projects which make these tests fail in an expected way, you can re-run the tests
 *       from IDE with -DUPDATE_TEST_SNAPSHOTS to update the files.
 *
 *       Or with bazel:
bazel test //tools/adt/idea/android:intellij.android.core.tests_tests__all --test_filter=LintModelSnapshotComparisonTest  \
--jvmopt="-DUPDATE_TEST_SNAPSHOTS=$(bazel info workspace)" --test_output=streamed
 */

@RunsInEdt
@RunWith(Parameterized::class)
class LintModelSnapshotComparisonTest : GradleIntegrationTest, SnapshotComparisonTest {

  data class TestProject(val template: String, val pathToOpen: String = "") {
    override fun toString(): String = "${template.removePrefix("projects/")}$pathToOpen"
  }

  @JvmField
  @Parameterized.Parameter
  var testProjectName: TestProject? = null

  companion object {
    @Suppress("unused")
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun testProjects(): Collection<*> = listOf(
      TestProject(TestProjectToSnapshotPaths.SIMPLE_APPLICATION),
      TestProject(TestProjectToSnapshotPaths.BASIC_CMAKE_APP),
      TestProject(TestProjectToSnapshotPaths.PSD_SAMPLE_GROOVY),
      TestProject(TestProjectToSnapshotPaths.MULTI_FLAVOR), // TODO(b/178796251): The snaphot does not include `proguardFiles`.
      TestProject(TestProjectToSnapshotPaths.COMPOSITE_BUILD),
      TestProject(TestProjectToSnapshotPaths.NON_STANDARD_SOURCE_SETS, "/application"),
      TestProject(TestProjectToSnapshotPaths.LINKED, "/firstapp"),
      TestProject(TestProjectToSnapshotPaths.KOTLIN_KAPT),
      TestProject("../projects/lintCustomChecks")
    )
  }


  @get:Rule
  val projectRule = AndroidProjectRule.withAndroidModels().onEdt()

  @get:Rule
  var testName = TestName()

  override fun getName(): String = testName.methodName
  override fun getBaseTestPath(): String = projectRule.fixture.tempDirPath
  override fun getTestDataDirectoryWorkspaceRelativePath(): String = "tools/adt/idea/android/testData/snapshots"
  override fun getAdditionalRepos(): Collection<File> =
    listOf(File(AndroidTestBase.getTestDataPath(), PathUtil.toSystemDependentName(TestProjectToSnapshotPaths.PSD_SAMPLE_REPO)))

  override val snapshotDirectoryWorkspaceRelativePath: String = "tools/adt/idea/android/testData/snapshots/lintModels"

  @Test
  fun testLintModels() {
    val projectName = testProjectName ?: error("unit test parameter not initialized")
    val root = prepareGradleProject(projectName.template, "project")
    openPreparedProject("project${testProjectName?.pathToOpen}") { project ->
      val dump = project.saveAndDump(mapOf("ROOT" to root)) { project, projectDumper -> projectDumper.dumpLintModels(project) }
      assertIsEqualToSnapshot(dump)
    }
  }
}
