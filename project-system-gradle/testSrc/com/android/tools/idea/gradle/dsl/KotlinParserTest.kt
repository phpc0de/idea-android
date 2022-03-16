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
package com.android.tools.idea.gradle.dsl

import com.android.tools.idea.gradle.dsl.api.ProjectBuildModel
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.PlatformTestCase
import junit.framework.TestCase
import org.junit.Test

class KotlinParserTest : PlatformTestCase() {
  val FN_BUILD_GRADLE_KTS = "build.gradle.kts"
  @Test
  fun testKotlinParserEnabledByDefault() {
    runWriteAction {
      getOrCreateProjectBaseDir()
      val modulePath = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(module.moduleNioFile.parent)!!
      val buildFile = modulePath.createChildData(this, FN_BUILD_GRADLE_KTS)
      VfsUtil.saveText(buildFile, """
        dependencies {
          testImplementation(project(":"))
        }
      """.trimIndent())
      TestCase.assertTrue(buildFile.isWritable)
    }

    val buildModel = ProjectBuildModel.get(myProject).getModuleBuildModel(myModule)
    // Check that something has been parsed
    val dependencies = buildModel!!.dependencies().all()
    assertNotEmpty(dependencies)
  }
}
