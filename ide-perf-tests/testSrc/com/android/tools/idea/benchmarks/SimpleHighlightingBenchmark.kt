/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.benchmarks

import com.android.tools.idea.testing.AndroidGradleProjectRule
import com.android.tools.idea.testing.TestProjectPaths
import com.android.tools.perflogger.Benchmark
import com.android.tools.perflogger.Metric
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.testFramework.runInEdtAndWait
import org.junit.Rule
import org.junit.Test

class SimpleHighlightingBenchmark {
  @get:Rule
  val gradleRule = AndroidGradleProjectRule()

  companion object {
    // Note: metadata for this benchmark is uploaded by IdeBenchmarkTestSuite.
    val benchmark = Benchmark.Builder("Highlighting simpleApplication")
      .setDescription("Syntax highlighting benchmark for a simple application.")
      .setProject(EDITOR_PERFGATE_PROJECT_NAME)
      .build()
  }

  @Test
  fun simpleProjectHighlighting() {
    disableExpensivePlatformAssertions(gradleRule.fixture)
    enableAllDefaultInspections(gradleRule.fixture)

    // Load project.
    gradleRule.load(TestProjectPaths.SIMPLE_APPLICATION)
    gradleRule.generateSources() // Gets us closer to a production setup.
    waitForAsyncVfsRefreshes() // Avoids write actions during highlighting.

    runInEdtAndWait {
      // Open editor.
      val fixture = gradleRule.fixture
      val project = gradleRule.project
      val projectDir = project.guessProjectDir()!!
      val javaFile = projectDir.findFileByRelativePath("app/src/main/java/google/simpleapplication/MyActivity.java")!!
      fixture.openFileInEditor(javaFile)

      // Measure.
      val samplesMs = measureTimeMs(
        warmupIterations = 10,
        mainIterations = 10,
        setUp = {
          PsiManager.getInstance(project).dropPsiCaches()
          System.gc()
        },
        action = {
          val info = fixture.doHighlighting(HighlightSeverity.ERROR)
          assert(info.isEmpty())
        }
      )
      val samplesStr = samplesMs.joinToString(prefix = "[", postfix = "]") { it.sampleData.toString() }
      println("Recorded samples: $samplesStr")

      // Save Perfgate data.
      val metric = Metric("highlighting_latency")
      metric.addSamples(benchmark, *samplesMs.toTypedArray())
      metric.commit()
    }
  }
}
