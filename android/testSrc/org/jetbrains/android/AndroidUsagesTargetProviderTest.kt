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
package org.jetbrains.android

import com.android.AndroidProjectTypes.PROJECT_TYPE_LIBRARY
import com.android.tools.idea.res.psi.ResourceReferencePsiElement
import com.android.tools.idea.testing.moveCaret
import com.google.common.truth.Truth.assertThat
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TestFixtureBuilder

class AndroidUsagesTargetProviderTest : AndroidTestCase() {

  private val MODULE_WITH_DEPENDENCY = "MODULE_WITH_DEPENDENCY"
  private val MODULE_WITHOUT_DEPENDENCY = "MODULE_WITHOUT_DEPENDENCY"
  private lateinit var MAIN_MODULE_COLOR_FILE : VirtualFile
  private lateinit var MAIN_MODULE_USAGE_COLOR_FILE : VirtualFile
  private lateinit var MODULE_WITH_DEPENDENCY_COLOR_FILE : VirtualFile
  private lateinit var MODULE_WITHOUT_DEPENDENCY_COLOR_FILE : VirtualFile

  private val COLORS_XML =
    //language=XML
    """
    <resources>
        <color name="testColor">#123456</color>
    </resources>
    """.trimIndent()

  override fun setUp() {
    super.setUp()
    MAIN_MODULE_COLOR_FILE = myFixture.addFileToProject("/res/values/colors.xml", COLORS_XML).virtualFile
    MAIN_MODULE_USAGE_COLOR_FILE = myFixture.addFileToProject(
      "/res/values/morecolors.xml",
      //language=XML
      """
      <resources>
          <color name="newColor">@color/testColor</color>
      </resources>
      """.trimIndent()
    ).virtualFile
    MODULE_WITH_DEPENDENCY_COLOR_FILE = myFixture.addFileToProject(
      getAdditionalModulePath(MODULE_WITH_DEPENDENCY) + "/res/values/colors.xml",
      COLORS_XML
    ).virtualFile
    MODULE_WITHOUT_DEPENDENCY_COLOR_FILE = myFixture.addFileToProject(
      getAdditionalModulePath(MODULE_WITHOUT_DEPENDENCY) + "/res/values/colors.xml",
      COLORS_XML
    ).virtualFile
  }

  override fun configureAdditionalModules(
    projectBuilder: TestFixtureBuilder<IdeaProjectTestFixture>,
    modules: MutableList<MyAdditionalModuleData>
  ) {
    addModuleWithAndroidFacet(projectBuilder, modules, MODULE_WITHOUT_DEPENDENCY, PROJECT_TYPE_LIBRARY, false)
    addModuleWithAndroidFacet(projectBuilder, modules, MODULE_WITH_DEPENDENCY, PROJECT_TYPE_LIBRARY, true)
  }

  private fun getElementFromTargetProvider() : ResourceReferencePsiElement {
    val targets = AndroidUsagesTargetProvider().getTargets(myFixture.editor, myFixture.file)
    assertThat(targets).hasLength(1)
    val primaryTarget = targets!![0]
    assertThat(primaryTarget).isInstanceOf(PsiElement2UsageTargetAdapter::class.java)
    val resourceReferencePsiElement = (primaryTarget as PsiElement2UsageTargetAdapter).element
    assertThat(resourceReferencePsiElement).isInstanceOf(ResourceReferencePsiElement::class.java)
    return resourceReferencePsiElement as ResourceReferencePsiElement
  }

  private fun checkContextElement() {
    val targetElement = getElementFromTargetProvider()
    val elementInFile = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
    val contextElement = targetElement.getCopyableUserData(ResourceReferencePsiElement.RESOURCE_CONTEXT_ELEMENT)
    assertThat(contextElement).isEqualTo(elementInFile)
  }

  fun testContextElement() {
    myFixture.configureFromExistingVirtualFile(MAIN_MODULE_COLOR_FILE)
    myFixture.moveCaret("name=\"testCo|lor\"")
    checkContextElement()

    myFixture.configureFromExistingVirtualFile(MAIN_MODULE_USAGE_COLOR_FILE)
    myFixture.moveCaret("@color/test|Color")
    checkContextElement()

    myFixture.configureFromExistingVirtualFile(MODULE_WITH_DEPENDENCY_COLOR_FILE)
    myFixture.moveCaret("name=\"testCo|lor\"")
    checkContextElement()

    myFixture.configureFromExistingVirtualFile(MODULE_WITHOUT_DEPENDENCY_COLOR_FILE)
    myFixture.moveCaret("name=\"testCo|lor\"")
    checkContextElement()
  }
}
