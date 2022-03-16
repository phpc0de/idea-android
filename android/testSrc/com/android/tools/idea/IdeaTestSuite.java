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
package com.android.tools.idea;

import com.android.testutils.JarTestSuiteRunner;
import com.android.tools.tests.GradleDaemonsRule;
import com.android.tools.tests.IdeaTestSuiteBase;
import com.android.tools.tests.LeakCheckerRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(JarTestSuiteRunner.class)
@JarTestSuiteRunner.ExcludeClasses({
  com.android.tools.idea.IdeaTestSuite.class,  // a suite mustn't contain itself
  com.android.tools.idea.rendering.RenderSecurityManagerTest.class,  // calls System.setSecurityManager
  com.android.tools.idea.testing.TestProjectPathsGeneratorTest.class, // This is for a standalone, test-only application

  // The following classes had failures when run in Bazel.
  com.android.tools.idea.gradle.project.sync.perf.GradleSyncPerfTest.class, // Sync performance test only runs on perf buildbot
  // Require resources with spaces (HTML File template)
  // https://github.com/bazelbuild/bazel/issues/374
  com.android.tools.idea.actions.annotations.InferSupportAnnotationsTest.class,
  org.jetbrains.android.dom.CreateMissingClassFixTest.class,
  com.android.tools.idea.testartifacts.instrumented.AndroidTestRunConfigurationTest.class // b/176807290
})
public class IdeaTestSuite extends IdeaTestSuiteBase {

  @ClassRule public static LeakCheckerRule checker = new LeakCheckerRule();

  @ClassRule public static GradleDaemonsRule gradle = new GradleDaemonsRule();

  static {
    try {
      unzipIntoOfflineMavenRepo("tools/base/build-system/studio_repo.zip");
      unzipIntoOfflineMavenRepo("tools/adt/idea/android/test_deps.zip");
      unzipIntoOfflineMavenRepo("tools/base/third_party/kotlin/kotlin-m2repository.zip");
      // Please consider adding tests that depend on specific versions of AGP into old-agp-tests instead of b/141628806
      unzipIntoOfflineMavenRepo("tools/data-binding/data_binding_runtime.zip");
    }
    catch (Throwable e) {
      // See b/143359533 for why we are handling errors here
      System.err.println("ERROR: Error initializing test suite, tests will likely fail following this error");
      e.printStackTrace();
    }
  }
}
