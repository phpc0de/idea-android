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
package com.android.tools.idea.uibuilder.visual

import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.android.resources.UiMode
import com.android.tools.idea.common.type.DesignerTypeRegistrar
import com.android.tools.idea.configurations.ConfigurationManager
import com.android.tools.idea.rendering.Locale
import com.android.tools.idea.uibuilder.LayoutTestCase
import com.android.tools.idea.uibuilder.type.LayoutFileType
import org.intellij.lang.annotations.Language
import org.junit.Ignore
import org.mockito.Mockito

class CustomModelsProviderTest : LayoutTestCase() {

  override fun setUp() {
    DesignerTypeRegistrar.register(LayoutFileType)
    super.setUp()
  }

  override fun tearDown() {
    super.tearDown()
    DesignerTypeRegistrar.clearRegisteredTypes()
    // Cleanup any added configurations
    VisualizationToolSettings.getInstance().globalState.customConfigurationAttributes = emptyList()
  }

  fun testCreateActions() {
    val file = myFixture.addFileToProject("/res/layout/test.xml", LAYOUT_FILE_CONTENT)

    val listener = Mockito.mock(ConfigurationSetListener::class.java)

    val modelsProvider = CustomModelsProvider(listener)
    val actions = modelsProvider.createActions(file, myFacet).getChildren(null)

    assertSize(1, actions)
    assertTrue(actions[0] is AddCustomConfigurationAction)
  }

  fun testOnlyCreateDefaultModel() {
    val file = myFixture.addFileToProject("/res/layout/test.xml", LAYOUT_FILE_CONTENT)

    val listener = Mockito.mock(ConfigurationSetListener::class.java)

    val modelsProvider = CustomModelsProvider(listener)
    val nlModels = modelsProvider.createNlModels(testRootDisposable, file, myFacet)
    val config = ConfigurationManager.getOrCreateInstance(myFacet).getConfiguration(file.virtualFile)

    assertSize(1, nlModels)
    assertEquals(config, nlModels[0].configuration)
  }

  fun testAddAndRemoveCustomConfig() {
    val file = myFixture.addFileToProject("/res/layout/test.xml", LAYOUT_FILE_CONTENT)

    val listener = Mockito.mock(ConfigurationSetListener::class.java)

    val modelsProvider = CustomModelsProvider(listener)
    val configurationManager = ConfigurationManager.getOrCreateInstance(myFacet)
    val defaultConfig = configurationManager.getConfiguration(file.virtualFile)

    val attributes = CustomConfigurationAttribute("Preview",
                                                  "pixel_3",
                                                  30,
                                                  ScreenOrientation.PORTRAIT,
                                                  Locale.ANY.toString(),
                                                  configurationManager.computePreferredTheme(defaultConfig),
                                                  UiMode.NORMAL,
                                                  NightMode.NOTNIGHT)
    modelsProvider.addCustomConfigurationAttributes(attributes)

    assertSize(1, modelsProvider.configurationAttributes)
    // The created nlModels contains default one plus all custom configurations,
    // so its size is CustomModelsProvider.customConfigurations.size() + 1.
    val nlModelsAfterAdded = modelsProvider.createNlModels(testRootDisposable, file, myFacet)
    assertSize(2, nlModelsAfterAdded)
    assertEquals(defaultConfig, nlModelsAfterAdded[0].configuration)
    assertEquals("Preview", nlModelsAfterAdded[1].modelDisplayName)

    modelsProvider.removeCustomConfigurationAttributes(nlModelsAfterAdded[1])
    assertSize(0, modelsProvider.configurationAttributes)
    val nlModelsAfterRemoved = modelsProvider.createNlModels(testRootDisposable, file, myFacet)
    assertSize(1, nlModelsAfterRemoved)
    assertEquals(defaultConfig, nlModelsAfterRemoved[0].configuration)
  }

  fun testAddCustomLocaleConfigLoadsCorrectFile() {
    val defaultFile = myFixture.addFileToProject("/res/layout/test.xml", LAYOUT_FILE_CONTENT)
    val enFile = myFixture.addFileToProject("/res/layout-en/test.xml", LAYOUT_FILE_CONTENT)

    val listener = Mockito.mock(ConfigurationSetListener::class.java)

    val modelsProvider = CustomModelsProvider(listener)
    val configurationManager = ConfigurationManager.getOrCreateInstance(myFacet)
    val defaultConfig = configurationManager.getConfiguration(defaultFile.virtualFile)

    val attributes = CustomConfigurationAttribute("Preview",
                                                  "pixel_3",
                                                  30,
                                                  ScreenOrientation.PORTRAIT,
                                                  Locale.create("en").toString(),
                                                  configurationManager.computePreferredTheme(defaultConfig),
                                                  UiMode.NORMAL,
                                                  NightMode.NOTNIGHT)
    modelsProvider.addCustomConfigurationAttributes(attributes)

    // Create models, first one is default one and second one is custom one, which should associate to en file.
    val nlModels = modelsProvider.createNlModels(testRootDisposable, defaultFile, myFacet)
    assertEquals(defaultFile.virtualFile, nlModels[0].virtualFile)
    assertEquals(enFile.virtualFile, nlModels[1].virtualFile)
  }

  fun testAddCustomOrientationConfigLoadsCorrectFile() {
    // Regression test for b/144923364.
    val defaultFile = myFixture.addFileToProject("/res/layout/test.xml", LAYOUT_FILE_CONTENT)
    val landFile = myFixture.addFileToProject("/res/layout-land/test.xml", LAYOUT_FILE_CONTENT)

    val listener = Mockito.mock(ConfigurationSetListener::class.java)

    val modelsProvider = CustomModelsProvider(listener)
    val configurationManager = ConfigurationManager.getOrCreateInstance(myFacet)
    val defaultConfig = configurationManager.getConfiguration(defaultFile.virtualFile)

    val attributes = CustomConfigurationAttribute("Preview",
                                                  "pixel_3",
                                                  30,
                                                  ScreenOrientation.LANDSCAPE,
                                                  Locale.ANY.toString(),
                                                  configurationManager.computePreferredTheme(defaultConfig),
                                                  UiMode.NORMAL,
                                                  NightMode.NOTNIGHT)
    modelsProvider.addCustomConfigurationAttributes(attributes)

    // Create models, first one is default one and second one is custom one, which should associate to en file.
    val nlModels = modelsProvider.createNlModels(testRootDisposable, defaultFile, myFacet)
    assertEquals(defaultFile.virtualFile, nlModels[0].virtualFile)
    assertEquals(landFile.virtualFile, nlModels[1].virtualFile)
  }
}

@Language("Xml")
private const val LAYOUT_FILE_CONTENT = """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:orientation="vertical">
</LinearLayout>
"""
