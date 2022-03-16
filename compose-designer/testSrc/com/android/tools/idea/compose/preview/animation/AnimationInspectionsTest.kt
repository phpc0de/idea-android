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
package com.android.tools.idea.compose.preview.animation

import com.android.tools.idea.compose.preview.addFileToProjectAndInvalidate
import com.android.tools.idea.testing.AndroidProjectRule
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ex.QuickFixWrapper
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


private const val UPDATE_TRANSITION_LABEL_NOT_SET_MESSAGE =
  "The label parameter should be set so this transition can be better inspected in the Animation Preview."

private const val TRANSITION_PROPERTY_LABEL_NOT_SET_MESSAGE =
  "The label parameter should be set so this transition property can be better inspected in the Animation Preview. " +
  "Otherwise, a default name will be used to represent the property."

class AnimationInspectionsTest {

  @get:Rule
  val projectRule: AndroidProjectRule = AndroidProjectRule.inMemory()
  private val fixture get() = projectRule.fixture

  @Before
  fun setUp() {
    fixture.addFileToProjectAndInvalidate(
      "src/androidx/compose/animation/core/Transition.kt",
      // language=kotlin
      """
      package androidx.compose.animation.core

      class Transition {}

      fun Transition.animateFloat(transitionSpec: () -> Unit, label: String = "FloatAnimation") {}

      fun <T> updateTransition(targetState: T, label: String? = null) {}
      """.trimIndent()
    )
    fixture.addFileToProjectAndInvalidate(
      "src/androidx/compose/animation/TransitionProperties.kt",
      // language=kotlin
      """
      package androidx.compose.animation

      fun Transition.animateColor(transitionSpec: () -> Unit, label: String = "ColorAnimation") {}

      fun animateColorAsState(transitionSpec: () -> Unit, label: String = "ColorState") {}
      """.trimIndent()
    )
    fixture.enableInspections(UpdateTransitionLabelInspection() as InspectionProfileEntry)
    fixture.enableInspections(TransitionPropertiesLabelInspection() as InspectionProfileEntry)
  }

  @Test
  fun testLabelNotSet() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition(targetState = false)
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertEquals(UPDATE_TRANSITION_LABEL_NOT_SET_MESSAGE, fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).single().description)
  }

  @Test
  fun testLabelSetExplicitly() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition(targetState = false, label = "explicit label")
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testLabelSetImplicitly() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition(targetState = false, "implicit label")
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testSetOtherParameterImplicitly() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition("this is the targetState")
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertEquals(UPDATE_TRANSITION_LABEL_NOT_SET_MESSAGE, fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).single().description)
  }

  @Test
  fun testAnimateFloatAnimationCorePackageLabelNotSet() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.animateFloat
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateFloat(transitionSpec = {})
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertEquals(TRANSITION_PROPERTY_LABEL_NOT_SET_MESSAGE, fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).single().description)
  }

  @Test
  fun testAnimateFloatAnimationCorePackageLabelSet() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.animateFloat
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateFloat(transitionSpec = {}, label = "float property")
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testAnimateColorAnimationPackageLabelNotSet() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.animateColor
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateColor(transitionSpec = {})
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertEquals(TRANSITION_PROPERTY_LABEL_NOT_SET_MESSAGE, fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).single().description)
  }

  @Test
  fun testAnimateColorAnimationPackageLabelSet() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.animateColor
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateColor(transitionSpec = {}, label = "color property")
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testAnimateColorCustomPackage() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateColor(transitionSpec = {})
      }

      fun Transition.animateColor(transitionSpec: (String) -> Unit, label: String = "ColorAnimation") {
        transitionSpec(label)
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    // The animateColor method is not defined in one of the Compose animation packages, so we don't show a warning.
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testAnimateColorAsStateAnimationPackage() {
    // language=kotlin
    val fileContent = """
      import androidx.compose.animation.animateColorAsState

      fun MyComposable() {
        animateColorAsState(transitionSpec = {})
      }
    """.trimIndent()

    fixture.configureByText("Test.kt", fileContent)
    // The animateColorAsState method is defined in one of the Compose animation packages, but it's not a Transition extension function so
    // we don't show a warning.
    assertTrue(fixture.doHighlighting(HighlightSeverity.WEAK_WARNING).isEmpty())
  }

  @Test
  fun testQuickFixUpdateTransition() {
    // language=kotlin
    val originalFileContent = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition(targetState = false)
      }
    """.trimIndent()
    fixture.configureByText("Test.kt", originalFileContent)

    // language=kotlin
    val fileContentAfterFix = """
      import androidx.compose.animation.core.updateTransition

      fun MyComposable() {
        updateTransition(targetState = false, label = "")
      }
    """.trimIndent()

    val quickFix = (fixture.getAllQuickFixes().single() as QuickFixWrapper).fix as LocalQuickFixOnPsiElement
    assertEquals("Add label parameter", quickFix.text)
    assertEquals("Compose preview", quickFix.familyName)

    ApplicationManager.getApplication().invokeAndWait {
      CommandProcessor.getInstance().executeCommand(fixture.project, { runWriteAction { quickFix.applyFix() } }, "Add Label Argument", null)
    }

    fixture.checkResult(fileContentAfterFix)
  }

  @Test
  fun testQuickFixTransitionProperty() {
    // language=kotlin
    val originalFileContent = """
      import androidx.compose.animation.core.animateFloat
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateFloat(transitionSpec = {})
      }
    """.trimIndent()
    fixture.configureByText("Test.kt", originalFileContent)

    // language=kotlin
    val fileContentAfterFix = """
      import androidx.compose.animation.core.animateFloat
      import androidx.compose.animation.core.Transition

      fun MyComposable() {
        val transition = Transition()
        transition.animateFloat(transitionSpec = {}, label = "")
      }
    """.trimIndent()

    val quickFix = (fixture.getAllQuickFixes().single() as QuickFixWrapper).fix as LocalQuickFixOnPsiElement
    assertEquals("Add label parameter", quickFix.text)
    assertEquals("Compose preview", quickFix.familyName)

    ApplicationManager.getApplication().invokeAndWait {
      CommandProcessor.getInstance().executeCommand(fixture.project, { runWriteAction { quickFix.applyFix() } }, "Add Label Argument", null)
    }

    fixture.checkResult(fileContentAfterFix)
  }
}