/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.kotlin;

import static com.android.tools.idea.wizard.template.Language.Java;
import static com.android.tools.idea.wizard.template.Language.Kotlin;
import static com.google.common.truth.Truth.assertThat;

import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.npw.NewModuleWizardFixture;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiTestRemoteRunner.class)
public class NewKotlinModuleTest {

  @Rule public final GuiTestRule guiTest = new GuiTestRule();

  private static final String APP_NAME = "app";
  private static final String NEW_KOTLIN_MODULE_NAME = "KotlinModule";

  @Test
  public void addNewKotlinModuleToNonKotlinProject() {
    createNewBasicProject(false);
    addNewKotlinModule();
  }

  @Test
  public void addNewKotlinModuleToKotlinProject() {
    createNewBasicProject(true);
    addNewKotlinModule();
  }

  private void createNewBasicProject(boolean hasKotlinSupport) {
    guiTest
      .welcomeFrame()
      .createNewProject()
      .clickNext()
      .getConfigureNewAndroidProjectStep()
      .enterName(APP_NAME)
      .enterPackageName("android.com")
      .setSourceLanguage(hasKotlinSupport ? Kotlin : Java)
      .wizard()
      .clickFinishAndWaitForSyncToFinish();

    if (hasKotlinSupport) {
      assertModuleSupportsKotlin(APP_NAME);
    }
    else {
      assertModuleDoesNotSupportKotlin(APP_NAME);
    }
  }

  private void addNewKotlinModule() {
    guiTest.ideFrame().openFromMenu(NewModuleWizardFixture::find, "File", "New", "New Module...")
      .clickNextPhoneAndTabletModule()
      .enterModuleName(NEW_KOTLIN_MODULE_NAME)
      .setSourceLanguage(Kotlin)
      .wizard()
      .clickNext() // Default options
      .clickNext() // Default Activity
      .clickFinishAndWaitForSyncToFinish();

    assertModuleSupportsKotlin(NEW_KOTLIN_MODULE_NAME);
  }

  private void assertModuleSupportsKotlin(String moduleName) {
    assertThat(guiTest.getProjectFileText(moduleName + "/build.gradle"))
      .contains("kotlin-android");
  }

  private void assertModuleDoesNotSupportKotlin(String moduleName) {
    assertThat(guiTest.getProjectFileText(moduleName + "/build.gradle"))
      .doesNotContain("kotlin");
  }
}
