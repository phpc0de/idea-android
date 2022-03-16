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
package com.android.tools.idea.compose.preview.runconfiguration

import com.android.AndroidProjectTypes
import com.android.tools.idea.compose.preview.addFileToProjectAndInvalidate
import com.android.tools.idea.flags.StudioFlags
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TestFixtureBuilder
import org.jetbrains.android.AndroidTestCase
import org.jetbrains.android.compose.stubComposableAnnotation
import org.jetbrains.android.compose.stubPreviewAnnotation
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComposePreviewRunConfigurationProducerTest : AndroidTestCase() {

  private lateinit var composableFunction: KtNamedFunction

  override fun setUp() {
    super.setUp()
    StudioFlags.COMPOSE_PREVIEW_RUN_CONFIGURATION.override(true)
    StudioFlags.COMPOSE_PREVIEW.override(true)
    myFixture.stubComposableAnnotation()
    myFixture.stubPreviewAnnotation()

    val file = myFixture.addFileToProjectAndInvalidate(
      "src/Test.kt",
      // language=kotlin
      """
        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.Composable

        @Composable
        @Preview
        fun Preview1() {
        }
      """.trimIndent())
    composableFunction = PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).first()
  }

  override fun tearDown() {
    super.tearDown()
    StudioFlags.COMPOSE_PREVIEW_RUN_CONFIGURATION.clearOverride()
    StudioFlags.COMPOSE_PREVIEW.clearOverride()
  }

  override fun configureAdditionalModules(projectBuilder: TestFixtureBuilder<IdeaProjectTestFixture>,
                                          modules: MutableList<MyAdditionalModuleData>) {
    super.configureAdditionalModules(projectBuilder, modules)
    addModuleWithAndroidFacet(projectBuilder, modules, "myLibrary", AndroidProjectTypes.PROJECT_TYPE_LIBRARY)
  }

  fun testSetupConfigurationFromContext() {
    val configurationFromFunction = createConfigurationFromElement(composableFunction)
    assertEquals("Preview1", configurationFromFunction.name)
    assertEquals("TestKt.Preview1", configurationFromFunction.composableMethodFqn)

    // Any PSI children of the KtNamedFunction (e.g. the keyword "fun", the function name, etc.) should produce a run configuration
    val configurationFromFunctionChildren = createConfigurationFromElement(composableFunction.children.random())
    assertEquals("Preview1", configurationFromFunctionChildren.name)
    assertEquals("TestKt.Preview1", configurationFromFunctionChildren.composableMethodFqn)

    assertEquals(2, composableFunction.annotationEntries.size)
    composableFunction.annotationEntries.forEach {
      // The run configuration can also be produced from the function's annotations
      val configurationFromAnnotation = createConfigurationFromElement(it)
      assertEquals("Preview1", configurationFromAnnotation.name)
      assertEquals("TestKt.Preview1", configurationFromAnnotation.composableMethodFqn)
    }
  }

  fun testParameterProvider() {
    val file = myFixture.addFileToProjectAndInvalidate(
      "src/TestPreviewParameter.kt",
      // language=kotlin
      """
        package my.composable.app

        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.ui.tooling.preview.PreviewParameter
        import androidx.compose.Composable

        class Names: CollectionPreviewParameterProvider<String>(listOf("Android", "Studio"))

        @Composable
        @Preview
        fun Preview1(@PreviewParameter(Names::class) name: String) {
        }
      """.trimIndent())

    val composableWithParameterProvider = PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).first()
    val configuration = createConfigurationFromElement(composableWithParameterProvider)
    assertEquals("Preview1", configuration.name)
    assertEquals("my.composable.app.TestPreviewParameterKt.Preview1", configuration.composableMethodFqn)
    // We should set the providerClassFqn value when running Compose Previews with a @PreviewParameter argument
    assertEquals("my.composable.app.Names", configuration.providerClassFqn)
  }

  fun testSetupConfigurationFromContextLibraryModule() {
    val modulePath = getAdditionalModulePath("myLibrary")

    val file = myFixture.addFileToProjectAndInvalidate(
      "$modulePath/src/main/java/com/example/mylibrary/TestLibraryFile.kt",
      // language=kotlin
      """
        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.Composable

        @Composable
        @Preview
        fun Preview1() {
        }
      """.trimIndent())

    // We shouldn't be able to produce ComposePreviewRunConfiguration from library modules
    val composableLibraryModule = PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).first()
    val configuration = createConfigurationFromElement(composableLibraryModule)
    assertEquals(newComposePreviewRunConfiguration().name, configuration.name)
    assertEquals(newComposePreviewRunConfiguration().composableMethodFqn, configuration.composableMethodFqn)
  }

  fun testInvalidContexts() {
    val file = myFixture.addFileToProjectAndInvalidate(
      "src/TestNotPreview.kt",
      // language=kotlin
      """
        import androidx.compose.ui.tooling.preview.Preview
        import androidx.compose.Composable

        @Preview
        @Composable
        fun Test() {
          fun NotAPreview() {
          }
        }

        @Preview
        @Composable
        fun Test() {
          @Preview
          @Composable
          fun NestedPreview() {
          }
        }
      """.trimIndent())

    val notPreview = PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).first { it.name == "NotAPreview" }
    val notPreviewConfiguration = createConfigurationFromElement(notPreview)
    assertEquals(newComposePreviewRunConfiguration().name, notPreviewConfiguration.name)
    assertEquals(newComposePreviewRunConfiguration().composableMethodFqn, notPreviewConfiguration.composableMethodFqn)

    val nestedPreview = PsiTreeUtil.findChildrenOfType(file, KtNamedFunction::class.java).first { it.name == "NestedPreview" }
    val nestedPreviewConfiguration = createConfigurationFromElement(nestedPreview)
    assertEquals(newComposePreviewRunConfiguration().name, nestedPreviewConfiguration.name)
    assertEquals(newComposePreviewRunConfiguration().composableMethodFqn, nestedPreviewConfiguration.composableMethodFqn)
  }

  fun testIsConfigurationFromContext() {
    val producer = ComposePreviewRunConfigurationProducer()
    val context = configurationContext(composableFunction)
    val runConfiguration = newComposePreviewRunConfiguration()

    assertFalse(producer.isConfigurationFromContext(runConfiguration, context))
    runConfiguration.name = "Preview1"
    assertFalse(producer.isConfigurationFromContext(runConfiguration, context))
    runConfiguration.composableMethodFqn = "TestKt.Preview1"
    // Configuration name does not need to match for the configuration be considered the same as the context's, as long as the composable
    // FQN does. That allows finding and reusing an existing configuration that runs a Compose Preview even if we don't know the actual
    // configuration name.
    assertTrue(producer.isConfigurationFromContext(runConfiguration, context))
    runConfiguration.name = "Preview2"
    assertTrue(producer.isConfigurationFromContext(runConfiguration, context))
  }

  private fun createConfigurationFromElement(element: PsiElement): ComposePreviewRunConfiguration {
    val context = configurationContext(element)
    val runConfiguration = newComposePreviewRunConfiguration()
    val producer = ComposePreviewRunConfigurationProducer()
    producer.setupConfigurationFromContext(runConfiguration, context, Ref(context.psiLocation))

    return runConfiguration
  }

  private fun newComposePreviewRunConfiguration() =
    ComposePreviewRunConfigurationType().configurationFactories[0].createTemplateConfiguration(project) as ComposePreviewRunConfiguration

  private fun configurationContext(element: PsiElement): ConfigurationContext {
    return ConfigurationContext(element)
  }
}
