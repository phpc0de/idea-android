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
package com.android.tools.idea.imports

import com.android.testutils.TestUtils
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.testing.AndroidGradleProjectRule
import com.android.tools.idea.testing.TestProjectPaths
import com.android.tools.idea.testing.loadNewFile
import com.android.tools.idea.testing.moveCaret
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.replaceService
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Tests for [AndroidMavenImportIntentionAction], for enabling [StudioFlags.SUGGESTED_IMPORT] specific tests.
 */
@RunsInEdt
class AndroidMavenImportIntentionActionSuggestedImportEnabledTest {
  private val projectRule = AndroidGradleProjectRule()

  @get:Rule
  val ruleChain = RuleChain.outerRule(projectRule).around(EdtRule())

  @Before
  fun setUp() {
    StudioFlags.ENABLE_SUGGESTED_IMPORT.override(true)
    ApplicationManager.getApplication().replaceService(
      MavenClassRegistryManager::class.java,
      fakeMavenClassRegistryManager,
      projectRule.fixture.testRootDisposable
    )
  }

  @After
  fun tearDown() {
    StudioFlags.ENABLE_SUGGESTED_IMPORT.clearOverride()
  }

  @Test
  fun testUnresolvedSymbol_nonAndroidX() {
    // Like testUnresolvedSymbolInKotlin but in a Java file
    projectRule.loadProject(TestProjectPaths.MIGRATE_TO_APP_COMPAT)
    assertBuildGradle(projectRule.project) { !it.contains("com.android.support:recyclerview-v7:") }
    projectRule.fixture.loadNewFile("app/src/main/java/test/pkg/imports/MainActivity2.java", """
      package test.pkg.imports;
      public class Test {
          private RecyclerView view;
      }
      """.trimIndent())

    val action = AndroidMavenImportIntentionAction()
    val element = projectRule.fixture.moveCaret("RecyclerView|")
    val available = action.isAvailable(projectRule.project, projectRule.fixture.editor, element)
    assertThat(available).isFalse()
  }

  @Test
  fun doNotSuggestIfAnyIsAlreadyDepended() {
    projectRule.loadProject(TestProjectPaths.ANDROIDX_SIMPLE) // this project uses AndroidX
    assertBuildGradle(projectRule.project) {
      !it.contains("androidx.palette:palette-ktx:") &&
      !it.contains("androidx.room:room-runtime:")
    }
    projectRule.fixture.loadNewFile(
      "app/src/main/java/test/pkg/imports/MainActivity2.kt",
      """
        package test.pkg.imports
        val someClass = FakeClass() // Here FakeClass is an unresolvable symbol
      """.trimIndent()
    )
    val source = projectRule.fixture.editor.document.text
    val action = AndroidMavenImportIntentionAction()
    var element = projectRule.fixture.moveCaret("FakeClass|()")
    var available = action.isAvailable(projectRule.project, projectRule.fixture.editor, element)
    assertThat(available).isTrue()
    // Since we have more than one suggestion, we just show general text `Add library dependency` here.
    assertThat(action.text).isEqualTo("Add library dependency and import")

    performAndWaitForSyncEnd(projectRule) {
      action.perform(projectRule.project, projectRule.fixture.editor, element, true)
    }

    // The deterministic order of suggestions are ensured, so the first option `androidx.palette:palette` is applied.
    assertBuildGradle(projectRule.project) {
      it.contains("implementation 'androidx.palette:palette-ktx:")
    }

    val newSource = projectRule.fixture.editor.document.text
    val diff = TestUtils.getDiff(source, newSource, 1)
    assertThat(diff.trim()).isEqualTo(
      """
      @@ -2 +2
        package test.pkg.imports
      +
      + import androidx.palette.graphics.FakeClass
      +
        val someClass = FakeClass() // Here FakeClass is an unresolvable symbol
      """.trimIndent().trim()
    )

    // Since we have added on `androidx.palette:palette`, no dependencies are to be suggested any more.
    element = projectRule.fixture.moveCaret("FakeClass|()")
    available = action.isAvailable(projectRule.project, projectRule.fixture.editor, element)
    assertThat(available).isFalse()
  }

  @Test
  fun doNotImportWhenAlreadyFullyQualifiedKotlin_userTypeCase() {
    // Like testDoNotImportWhenAlreadyFullyQualifiedJava, but for Kotlin
    // Like testUnresolvedSymbolInKotlin, but in an AndroidX project (so the artifact name
    // must be mapped both in the display name and in the dependency inserted into the build.gradle file)
    projectRule.loadProject(TestProjectPaths.ANDROIDX_SIMPLE) // this project uses AndroidX
    assertBuildGradle(projectRule.project) { !it.contains("androidx.camera:camera-core:") }
    projectRule.fixture.loadNewFile("app/src/main/java/test/pkg/imports/MainActivity2.kt", """
      package test.pkg.imports
      val builder = object : androidx.camera.core.ExtendableBuilder { // Here `camera` (package segment) is an unresolvable symbol
      """.trimIndent())
    val source = projectRule.fixture.editor.document.text

    val action = AndroidMavenImportIntentionAction()
    val element = projectRule.fixture.moveCaret("came|ra")
    val available = action.isAvailable(projectRule.project, projectRule.fixture.editor, element)
    assertThat(available).isTrue()
    assertThat(action.text).isEqualTo("Add dependency on androidx.camera:camera-core (alpha) and import")
    // Note: We do perform, not performAndSync here, since the androidx libraries aren't available
    // in the test prebuilts right now
    performWithoutSync(projectRule, action, element)

    assertBuildGradle(projectRule.project) { it.contains("implementation 'androidx.camera:camera-core:") }

    // Make sure we haven't added an import statement since the reference is already fully qualified
    val newSource = projectRule.fixture.editor.document.text
    assertThat(source).isEqualTo(newSource)
  }

  @Test
  fun doNotImportWhenAlreadyFullyQualifiedKotlin_userTypeCase_nestedClass() {
    // Like testDoNotImportWhenAlreadyFullyQualifiedJava, but for Kotlin
    // Like testUnresolvedSymbolInKotlin, but in an AndroidX project (so the artifact name
    // must be mapped both in the display name and in the dependency inserted into the build.gradle file)
    projectRule.loadProject(TestProjectPaths.ANDROIDX_SIMPLE) // this project uses AndroidX
    assertBuildGradle(projectRule.project) { !it.contains("androidx.camera:camera-core:") }
    projectRule.fixture.loadNewFile("app/src/main/java/test/pkg/imports/MainActivity2.kt", """
      package test.pkg.imports
      val callback = object : androidx.camera.core.ImageCapture.OnImageSavedCallback { // Here `camera` is an unresolvable symbol
      """.trimIndent())
    val source = projectRule.fixture.editor.document.text

    val action = AndroidMavenImportIntentionAction()
    val element = projectRule.fixture.moveCaret("came|ra")
    val available = action.isAvailable(projectRule.project, projectRule.fixture.editor, element)
    assertThat(available).isTrue()
    assertThat(action.text).isEqualTo("Add dependency on androidx.camera:camera-core (alpha) and import")
    // Note: We do perform, not performAndSync here, since the androidx libraries aren't available
    // in the test prebuilts right now
    performWithoutSync(projectRule, action, element)

    assertBuildGradle(projectRule.project) { it.contains("implementation 'androidx.camera:camera-core:") }

    // Make sure we haven't added an import statement since the reference is already fully qualified
    val newSource = projectRule.fixture.editor.document.text
    assertThat(source).isEqualTo(newSource)
  }
}