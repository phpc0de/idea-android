/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.inspectors.common.api.ide

import com.android.testutils.JarTestSuiteRunner
import com.android.tools.tests.IdeaTestSuiteBase
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl
import com.intellij.testFramework.TestRunnerUtil
import org.junit.runner.RunWith

@RunWith(JarTestSuiteRunner::class)
@JarTestSuiteRunner.ExcludeClasses(InspectorsCommonApiIdeTestSuite::class) // a suite mustn't contain itself
class InspectorsCommonApiIdeTestSuite : IdeaTestSuiteBase() {
  init {
    TestRunnerUtil.replaceIdeEventQueueSafely()
    (PersistentFS.getInstance() as PersistentFSImpl).cleanPersistedContents()
  }
}