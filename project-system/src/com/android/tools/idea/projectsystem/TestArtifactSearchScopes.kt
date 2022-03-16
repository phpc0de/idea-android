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
package com.android.tools.idea.projectsystem

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.io.File

/**
 * Per-module helper for determining if a file is a test and if so, which kind.
 */
interface TestArtifactSearchScopes {
  /** Returns a [Module] that this search scopes belong to. */
  val module: Module

  /** Returns a [GlobalSearchScope] that contains android tests of the given module.  */
  val androidTestSourceScope: GlobalSearchScope

  /** Returns a [GlobalSearchScope] that contains unit tests of the given module.  */
  val unitTestSourceScope: GlobalSearchScope

  /** Returns a [GlobalSearchScope] that contains files to be excluded from resolution inside android tests. */
  val androidTestExcludeScope: GlobalSearchScope

  /** Returns a [GlobalSearchScope] that contains files to be excluded from resolution inside unit tests. */
  val unitTestExcludeScope: GlobalSearchScope

  /**
   *  Returns a [GlobalSearchScope] that contains files to be excluded from resolution inside files that belong to both unit test and
   *  android test.
   *
   *  Note that AGP doesn't support shared tests.
   */
  val sharedTestExcludeScope: GlobalSearchScope

  /** Checks if the given file is an android test. */
  fun isAndroidTestSource(file: VirtualFile): Boolean

  /** Checks if the given file is a unit test. */
  fun isUnitTestSource(file: VirtualFile): Boolean

  /**
   * Checks if the given file should be on the unit testing classpath.
   *
   * This is similar to checking if [unitTestExcludeScope] contains the file, but handles the case where the file does not exist yet, so it
   * cannot be represented as a [VirtualFile].
   */
  fun includeInUnitTestClasspath(file: File): Boolean

  companion object {
    /** Returns a [TestArtifactSearchScopes] instance for a given [module] or null the module doesn't support separate test artifacts. */
    @JvmStatic
    fun getInstance(module: Module) = module.getModuleSystem().getTestArtifactSearchScopes()
  }
}
