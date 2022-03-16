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
package com.android.tools.idea.gradle.project.sync

import com.android.sdklib.devices.Abi
import com.android.sdklib.devices.Abi.ARMEABI_V7A
import com.android.sdklib.devices.Abi.X86
import com.android.tools.idea.gradle.project.facet.ndk.NdkFacet
import com.android.tools.idea.run.AndroidRunConfiguration
import com.android.tools.idea.run.DeviceFutures
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.GradleIntegrationTest
import com.android.tools.idea.testing.TestProjectPaths
import com.android.tools.idea.testing.gradleModule
import com.android.tools.idea.testing.openPreparedProject
import com.android.tools.idea.testing.prepareGradleProject
import com.google.common.truth.Truth.assertThat
import com.intellij.execution.RunManager
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File

class MakeBeforeRunTaskProviderIntegrationTest : GradleIntegrationTest {

  @get:Rule
  val projectRule = AndroidProjectRule.withAndroidModels()

  @get:Rule
  var testName = TestName()

  @Test
  fun testModelsAreNotFetchedForSyncedAbi() {
    prepareGradleProject(TestProjectPaths.DEPENDENT_NATIVE_MODULES, "project")
    openPreparedProject("project") { project ->
      val ndkFacet = NdkFacet.getInstance(project.gradleModule(":app") ?: error(":app module not found"))
      val selectedVariant = ndkFacet?.selectedVariantAbi

      assertThat(selectedVariant?.abi).isEqualTo(X86.toString())
      assertThat(ndkFacet?.ndkModuleModel?.ndkModel?.syncedVariantAbis?.map { it.abi }).containsExactly(X86.toString())

      fun attemptRunningOn(abi: Abi) {
        // Note: This is verified to still work in MakeBeforeRunTaskProviderIntegration35Test.
        withSimulatedSyncError(errorMessage) {
          val runConfiguration = RunManager.getInstance(project).allConfigurationsList.filterIsInstance<AndroidRunConfiguration>().single()
          runConfiguration.executeMakeBeforeRunStepInTest(DeviceFutures.forDevices(listOf(mockDeviceFor(23, listOf(abi)))))
        }
      }

      // Running on X86 should not require any additional models.
      attemptRunningOn(X86)
    }
  }

  @Test
  fun testModelsAreFetchedForNotSyncedAbi() {
    prepareGradleProject(TestProjectPaths.DEPENDENT_NATIVE_MODULES, "project")
    openPreparedProject("project") { project ->
      val ndkFacet = NdkFacet.getInstance(project.gradleModule(":app") ?: error(":app module not found"))
      val selectedVariant = ndkFacet?.selectedVariantAbi

      assertThat(selectedVariant?.abi).isEqualTo(X86.toString())
      assertThat(ndkFacet?.ndkModuleModel?.ndkModel?.syncedVariantAbis?.map { it.abi }).containsExactly(X86.toString())

      fun attemptRunningOn(abi: Abi) {
        // Note: This is verified to still work in MakeBeforeRunTaskProviderIntegration35Test.
        withSimulatedSyncError(errorMessage) {
          val runConfiguration = RunManager.getInstance(project).allConfigurationsList.filterIsInstance<AndroidRunConfiguration>().single()
          runConfiguration.executeMakeBeforeRunStepInTest(DeviceFutures.forDevices(listOf(mockDeviceFor(23, listOf(abi)))))
        }
      }

      // Even running a not synced ABI should not require sync with native sync v2.
      attemptRunningOn(ARMEABI_V7A)

      assertThat(ndkFacet?.ndkModuleModel?.ndkModel?.syncedVariantAbis?.map { it.abi })
        .containsExactly(X86.toString(), ARMEABI_V7A.toString())
    }
  }

  override fun getName(): String = testName.methodName
  override fun getBaseTestPath(): String = projectRule.fixture.tempDirPath
  override fun getTestDataDirectoryWorkspaceRelativePath(): String = TestProjectPaths.TEST_DATA_PATH
  override fun getAdditionalRepos(): Collection<File> = listOf()
}

private const val errorMessage: String = "Unexpected attempt to resolve a project."
