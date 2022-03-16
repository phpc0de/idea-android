/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.gradle;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.idea.gradle.dsl.model.dependencies.ExpectedModuleDependency;
import com.android.tools.idea.tests.gui.framework.GuiTestRule;
import com.android.tools.idea.tests.gui.framework.fixture.RenameModuleDialogFixture;
import com.android.tools.idea.tests.gui.framework.fixture.SelectRefactoringDialogFixture;
import com.android.tools.idea.tests.gui.framework.fixture.gradle.GradleBuildModelFixture;
import com.intellij.testGuiFramework.framework.GuiTestRemoteRunner;
import java.io.IOException;
import org.fest.swing.core.KeyPressInfo;
import org.fest.swing.util.Platform;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiTestRemoteRunner.class)
public class GradleRenameModuleTest {

  @Rule public final GuiTestRule guiTest = new GuiTestRule();

  @Test
  public void testRenameModule() throws IOException {
    guiTest
      .importSimpleApplication()
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.getProjectView()
          .selectProjectPane()
          .clickPath("SimpleApplication", "app")
          .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
          .selectRenameModule()
          .clickOk()
          .enterText("app2")
          .clickOk();
      });
    assertThat(guiTest.ideFrame().hasModule("app2")).named("app2 module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("app")).named("app module exists").isFalse();
  }

  @Test
  public void testRenameModuleAlsoChangeReferencesInBuildFile() throws IOException {
    guiTest.importMultiModule()
      .getProjectView()
      .selectProjectPane()
      .clickPath("MultiModule", "library")
      .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
      .selectRenameModule()
      .clickOk()
      .enterText("newLibrary")
      .clickOk();

    guiTest.waitForBackgroundTasks();
    assertThat(guiTest.ideFrame().hasModule("newLibrary")).named("newLibrary module exists").isTrue();

    // app module has two references to library module
    GradleBuildModelFixture buildModel = guiTest.ideFrame().parseBuildFileForModule("app");

    ExpectedModuleDependency expected = new ExpectedModuleDependency();
    expected.configurationName = "debugImplementation";
    expected.path = ":newLibrary";
    buildModel.requireDependency(expected);

    expected.configurationName = "releaseImplementation";
    buildModel.requireDependency(expected);
  }

  @Test
  public void testCannotRenameRootModule() throws IOException {
    guiTest.importSimpleApplication()
      .getProjectView()
      .selectProjectPane()
      .clickPath("SimpleApplication")
      .openFromMenu(RenameModuleDialogFixture::find, "Refactor", "Rename...")
      .enterText("SimpleApplication2")
      .clickOkAndRequireError("Can't rename root module");
  }

  @Test
  public void testCannotRenameToExistingFile() throws IOException {
    guiTest.importMultiModule()
      .getProjectView()
      .selectProjectPane()
      .clickPath("MultiModule", "app")
      .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
      .selectRenameModule()
      .clickOk()
      .enterText("library2")
      .clickOkAndRequireError("Module named 'library2' already exist")
      .clickCancel();
  }

  @Test
  public void testUndoKeyAfterRenameModule() throws IOException {
    guiTest
      .importSimpleApplication()
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.getProjectView()
          .selectProjectPane()
          .clickPath("SimpleApplication", "app")
          .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
          .selectRenameModule()
          .clickOk()
          .enterText("app2")
          .clickOk();
      });
    assertThat(guiTest.ideFrame().hasModule("app2")).named("app2 module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("app")).named("app module exists").isFalse();

    KeyPressInfo undo = KeyPressInfo.keyCode(java.awt.event.KeyEvent.VK_Z).modifiers(Platform.controlOrCommandMask());
    guiTest
      .ideFrame()
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.pressAndReleaseKey(undo);
      });

    assertThat(guiTest.ideFrame().hasModule("app")).named("app module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("app2")).named("app2 module exists").isFalse();
  }

  @Test
  public void testUndoMenuAfterRenameModule() throws IOException {
    guiTest
      .importSimpleApplication()
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.getProjectView()
          .selectProjectPane()
          .clickPath("SimpleApplication", "app")
          .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
          .selectRenameModule()
          .clickOk()
          .enterText("app2")
          .clickOk();
      });
    assertThat(guiTest.ideFrame().hasModule("app2")).named("app2 module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("app")).named("app module exists").isFalse();

    guiTest
      .ideFrame()
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.invokeMenuPath("Edit", "Undo Renaming module app");
      });

    assertThat(guiTest.ideFrame().hasModule("app")).named("app module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("app2")).named("app2 module exists").isFalse();
  }

  @Test
  public void testRenameDynamicFeatureModule() throws IOException {
    guiTest
      .importProjectAndWaitForProjectSyncToFinish("DynamicFeatureApplication")
      .actAndWaitForGradleProjectSyncToFinish(it -> {
        it.getProjectView()
          .selectProjectPane()
          .clickPath("DynamicFeatureApplication", "dynamicFeature")
          .openFromMenu(SelectRefactoringDialogFixture::find, "Refactor", "Rename...")
          .selectRenameModule()
          .clickOk()
          .enterText("dF2")
          .clickOk();
      });
    assertThat(guiTest.ideFrame().hasModule("dF2")).named("dF2 module exists").isTrue();
    assertThat(guiTest.ideFrame().hasModule("dynamicFeature")).named("dynamicFeature module exists").isFalse();

    GradleBuildModelFixture buildModel = guiTest.ideFrame().parseBuildFileForModule("app");
    buildModel.requireDynamicFeature(":dF2");
  }
}
