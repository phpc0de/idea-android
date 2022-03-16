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
package com.android.tools.idea.navigator.nodes.android

import com.android.tools.idea.testing.AndroidModuleModelBuilder
import com.android.tools.idea.testing.AndroidProjectBuilder
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.JavaModuleModelBuilder.Companion.rootModuleBuilder
import com.android.tools.idea.testing.onEdt
import com.intellij.ide.projectView.ViewSettings
import com.intellij.testFramework.RunsInEdt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@RunsInEdt
class AndroidBuildScriptsGroupNodeTest {
  @get:Rule
  val projectRule = AndroidProjectRule.withAndroidModels().onEdt()

  @Test
  fun rootProjectOnly() {
    val setting_gradle = projectRule.fixture.addFileToProject("settings.gradle", "")
    val build_gradle = projectRule.fixture.addFileToProject("build.gradle", "")
    val app_build_gradle = projectRule.fixture.addFileToProject("app/build.gradle", "")
    val gradle_wrapper_gradle_wrapper_properties = projectRule.fixture.addFileToProject("gradle/wrapper/gradle-wrapper.properties", "")
    val gradle_properties = projectRule.fixture.addFileToProject("gradle.properties", "")
    val local_properties = projectRule.fixture.addFileToProject("local.properties", "")
    val unrelated_txt = projectRule.fixture.addFileToProject("unrelated.txt", "")
    projectRule.setupProjectFrom(rootModuleBuilder)

    val node = AndroidBuildScriptsGroupNode(projectRule.project, ViewSettings.DEFAULT)

    assertTrue(node.contains(setting_gradle.virtualFile))
    assertTrue(node.contains(build_gradle.virtualFile))
    assertTrue(node.contains(gradle_wrapper_gradle_wrapper_properties.virtualFile))
    assertTrue(node.contains(gradle_properties.virtualFile))
    assertTrue(node.contains(local_properties.virtualFile))
    assertFalse(node.contains(app_build_gradle.virtualFile))  // We do not show build files from modules not recognised by sync.
    assertFalse(node.contains(unrelated_txt.virtualFile))
  }

  @Test
  fun appProject() {
    val setting_gradle = projectRule.fixture.addFileToProject("settings.gradle", "")
    val build_gradle = projectRule.fixture.addFileToProject("build.gradle", "")
    val app_build_gradle = projectRule.fixture.addFileToProject("app/build.gradle", "")
    val app_proguard_rules_pro = projectRule.fixture.addFileToProject("app/proguard-rules.pro", "")
    val app_consumer_proguard_rules_pro = projectRule.fixture.addFileToProject("app/consumer-proguard-rules.pro", "")
    val unrelated_txt = projectRule.fixture.addFileToProject("unrelated.txt", "")
    val app_unrelated_txt = projectRule.fixture.addFileToProject("app/unrelated.txt", "")
    projectRule.setupProjectFrom(rootModuleBuilder,  AndroidModuleModelBuilder(":app", "debug", AndroidProjectBuilder()))

    val node = AndroidBuildScriptsGroupNode(projectRule.project, ViewSettings.DEFAULT)

    assertTrue(node.contains(setting_gradle.virtualFile))
    assertTrue(node.contains(build_gradle.virtualFile))
    assertTrue(node.contains(app_proguard_rules_pro.virtualFile))
    assertTrue(node.contains(app_consumer_proguard_rules_pro.virtualFile))
    assertTrue(node.contains(app_build_gradle.virtualFile))
    assertFalse(node.contains(unrelated_txt.virtualFile))
    assertFalse(node.contains(app_unrelated_txt.virtualFile))
  }
}